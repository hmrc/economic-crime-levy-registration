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

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import cats.data.EitherT
import play.api.http.Status.BAD_GATEWAY
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.DmsConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.DeRegistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DmsSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.economiccrimelevyregistration.utils.PdfGenerator.buildPdf
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
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

  def submitToDms(base64EncodedDmsSubmissionHtml: Option[String], now: Instant, registrationType: RegistrationType)(
    implicit hc: HeaderCarrier
  ): EitherT[Future, DmsSubmissionError, CreateEclSubscriptionResponsePayload] =
    for {
      pdf      <- createPdf(base64EncodedDmsSubmissionHtml)
      body     <- createBody(pdf, now, registrationType)
      response <- sendPdf(body, now)(hc)
    } yield response

  private def createPdf(
    base64EncodedDmsSubmissionHtml: Option[String]
  ): EitherT[Future, DmsSubmissionError, ByteArrayOutputStream] =
    EitherT {
      Future.successful(
        base64EncodedDmsSubmissionHtml match {
          case Some(value) =>
            Try(new String(Base64.getDecoder.decode(value))) match {
              case Success(result) =>
                Try(buildPdf(result)) match {
                  case Success(pdfResult) => Right(pdfResult)
                  case Failure(e)         => Left(DmsSubmissionError.InternalUnexpectedError(Some(e.getCause)))
                }
              case Failure(e)      => Left(DmsSubmissionError.InternalUnexpectedError(Some(e.getCause)))
            }
          case None        =>
            Left(DmsSubmissionError.BadGateway("base64EncodedDmsSubmissionHtml field not provided", BAD_GATEWAY))
        }
      )
    }

  private def createBody(
    pdf: ByteArrayOutputStream,
    now: Instant,
    registrationType: RegistrationType
  ) =
    EitherT {
      Future.successful(
        Try(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            LocalDateTime.ofInstant(now, ZoneOffset.UTC)
          )
        ) match {
          case Success(result) => Right(assembleBodySource(pdf, result, registrationType))
          case Failure(e)      => Left(DmsSubmissionError.InternalUnexpectedError(Some(e.getCause)))
        }
      )
    }

  private def assembleBodySource(
    pdf: ByteArrayOutputStream,
    dateOfReceipt: String,
    registrationType: RegistrationType
  ): Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed] = {
    val formId =
      if (registrationType == DeRegistration) {
        appConfig.dmsSubmissionDeregistrationFormId
      } else { appConfig.dmsSubmissionFormId }

    Source(
      Seq(
        DataPart("callbackUrl", appConfig.dmsSubmissionCallbackUrl),
        DataPart("metadata.source", appConfig.dmsSubmissionSource),
        DataPart("metadata.timeOfReceipt", dateOfReceipt),
        DataPart("metadata.formId", formId),
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
  }

  def sendPdf(
    body: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed],
    now: Instant
  )(implicit hc: HeaderCarrier): EitherT[Future, DmsSubmissionError, CreateEclSubscriptionResponsePayload] =
    EitherT {
      dmsConnector
        .sendPdf(body)
        .map(_ => Right(CreateEclSubscriptionResponsePayload(now, "")))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DmsSubmissionError.BadGateway(reason = message, code = code))
          case NonFatal(thr) => Left(DmsSubmissionError.InternalUnexpectedError(Some(thr)))
        }
    }
}
