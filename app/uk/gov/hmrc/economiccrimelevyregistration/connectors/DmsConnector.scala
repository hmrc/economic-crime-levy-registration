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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import play.api.http.Status.ACCEPTED
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.HttpEntity
import play.api.mvc.{ResponseHeader, Result}
import play.mvc.Results.status

import scala.concurrent.duration.Duration

@Singleton
class DmsConnector @Inject()(
  httpClient: HttpClientV2,
  servicesConfig: ServicesConfig,
  configuration: Config
)(implicit ec: ExecutionContext) {

  val dmsBaseUrl: String = servicesConfig.baseUrl("dms")

  def sendPdf(pdf: ByteArrayOutputStream, instant: Instant)(implicit
    hc: HeaderCarrier
  ): Future[Boolean] = {
    val timeouts = configuration.getString("microservice.services.dms.timeouts").split(" ")
    val clientAuthToken = configuration.getString("microservice.services.internal-auth.token")
    val appName = configuration.getString("appName")
    val dateOfReceipt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
      LocalDateTime.ofInstant(instant.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
    )

    val body = Source(Seq(
      DataPart("callbackUrl", configuration.getString("microservice.services.dms.callback")),
      DataPart("metadata.source", appName),
      DataPart("metadata.timeOfReceipt", dateOfReceipt),
      DataPart("metadata.formId", "ECL Registration"),
      DataPart("metadata.customerId", appName),
      DataPart("metadata.submissionMark", appName),
      DataPart("metadata.classificationType", configuration.getString("microservice.services.dms.class")),
      DataPart("metadata.businessArea", appName),
      FilePart(
        key = "form",
        filename = "form.pdf",
        contentType = Some("application/pdf"),
        ref = Source.single(ByteString(pdf.toByteArray))
      )
    ))
    for (timeout <- timeouts) {
      val result: Future[Result] = httpClient.post(new URL(dmsBaseUrl + "/dms-submission/submit"))
        .setHeader(AUTHORIZATION -> clientAuthToken)
        .withBody(body)
        .execute.map(r => Result(ResponseHeader(r.status, Map()), HttpEntity.NoEntity))
      if (Await.result(result, Duration(timeout)).header.status == ACCEPTED) {
        return Future.successful(true)
      }
    }
    Future.successful(false)
  }
}
