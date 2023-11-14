/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.services

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.DmsConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DmsSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.economiccrimelevyregistration.utils.PdfGenerator.buildPdf
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

@Singleton
class DmsService @Inject() (
  dmsConnector: DmsConnector,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) extends ErrorHandler {

  def submitToDms(base64EncodedDmsSubmissionHtml: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, DmsSubmissionError, CreateEclSubscriptionResponsePayload] =
    for {
      pdf      <- createPdf(base64EncodedDmsSubmissionHtml)
      body     <- createBody(pdf)
      response <- sendPdf(body)(hc)
    } yield response

  def createPdf(base64EncodedDmsSubmissionHtml: String): EitherT[Future, DmsSubmissionError, ByteArrayOutputStream] =
    EitherT {
      Future.successful(
        Try(new String(Base64.getDecoder.decode(base64EncodedDmsSubmissionHtml))) match {
          case Success(result) =>
            Try(buildPdf(result)) match {
              case Success(pdfResult) => Right(pdfResult)
              case Failure(e)         => Left(DmsSubmissionError.InternalUnexpectedError(e.getMessage, Some(e.getCause)))
            }
          case Failure(e)      => Left(DmsSubmissionError.InternalUnexpectedError(e.getMessage, Some(e.getCause)))
        }
      )
    }

  def createBody(
    pdf: ByteArrayOutputStream
  ): EitherT[Future, DmsSubmissionError, Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed]] =
    EitherT {
      Future.successful(
        Try(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
          )
        ) match {
          case Success(result) => Right(assembleBodySource(pdf, result))
          case Failure(e)      => Left(DmsSubmissionError.InternalUnexpectedError(e.getMessage, Some(e.getCause)))
        }
      )
    }

  def assembleBodySource(pdf: ByteArrayOutputStream, dateOfReceipt: String) =
    Source(
      Seq(
        DataPart("callbackUrl", appConfig.dmsSubmissionCallbackUrl),
        DataPart("metadata.source", appConfig.dmsSubmissionSource),
        DataPart("metadata.timeOfReceipt", dateOfReceipt),
        DataPart("metadata.formId", appConfig.dmsSubmissionFormId),
        DataPart("metadata.customerId", appConfig.dmsSubmissionCustomerId),
        DataPart("metadata.classificationType", appConfig.dmsSubmissionClassificationType),
        DataPart("metadata.businessArea", appConfig.dmsSubmissionBusinessArea),
        FilePart(
          key = "form",
          filename = "form.pdf",
          contentType = Some("application/pdf"),
          ref = Source.single(ByteString(pdf.toByteArray))
        )
      )
    )

  def sendPdf(
    body: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed]
  )(implicit hc: HeaderCarrier): EitherT[Future, DmsSubmissionError, CreateEclSubscriptionResponsePayload] =
    EitherT {
      Future.successful(
        dmsConnector
          .sendPdf(body)
          .map(Right(CreateEclSubscriptionResponsePayload(Instant.now(), "")))
          .recover {
            case error @ UpstreamErrorResponse(message, code, _, _)
                if UpstreamErrorResponse.Upstream5xxResponse
                  .unapply(error)
                  .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
              Left(DmsSubmissionError.BadGateway(reason = message, code = code))
            case NonFatal(thr) => Left(DmsSubmissionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
          }
      )
    }
}
