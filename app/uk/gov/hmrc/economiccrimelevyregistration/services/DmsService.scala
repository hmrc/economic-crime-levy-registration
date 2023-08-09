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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.DmsConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.economiccrimelevyregistration.utils.PdfGenerator.buildPdf
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsService @Inject() (
  dmsConnector: DmsConnector,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) {

  def submitToDms(optBase64EncodedDmsSubmissionHtml: Option[String], now: Instant)(implicit
    hc: HeaderCarrier
  ): Future[CreateEclSubscriptionResponsePayload] = {
    val base64EncodedDmsSubmissionHtml: String = optBase64EncodedDmsSubmissionHtml.getOrElse(
      throw new IllegalStateException("Base64 encoded DMS submission HTML not found in registration data")
    )
    val html                                   = new String(Base64.getDecoder.decode(base64EncodedDmsSubmissionHtml))
    val pdf                                    = buildPdf(html)

    val dateOfReceipt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
      LocalDateTime.ofInstant(now.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
    )

    val body = Source(
      Seq(
        DataPart("callbackUrl", appConfig.dmsSubmissionCallbackUrl),
        DataPart("metadata.source", appConfig.dmsSubmissionSource),
        DataPart("metadata.timeOfReceipt", dateOfReceipt),
        DataPart("metadata.formId", appConfig.dmsSubmissionFormId),
        DataPart("metadata.customerId", appConfig.dmsSubmissionCustomerId),
        DataPart("metadata.submissionMark", appConfig.dmsSubmissionSubmissionMark),
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

    dmsConnector.sendPdf(body).map {
      case true  => CreateEclSubscriptionResponsePayload(now, "")
      case false => throw new IllegalStateException("Could not send PDF to DMS queue")
    }
  }
}
