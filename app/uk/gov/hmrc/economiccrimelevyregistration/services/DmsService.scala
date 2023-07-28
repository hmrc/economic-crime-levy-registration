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

import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.NrsConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, CreateEclSubscriptionResponsePayload}
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.utils.PdfGenerator.buildPdf
import uk.gov.hmrc.http.HeaderCarrier

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import java.time.{Clock, Instant}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsService @Inject()(nrsConnector: NrsConnector, clock: Clock)(implicit
                                                                     ec: ExecutionContext
) extends Logging {

  def submitToDms(optBase64EncodedDmsSubmissionHtml: Option[String])(implicit
    hc: HeaderCarrier,
    request: AuthorisedRequest[_]
  ): Future[CreateEclSubscriptionResponsePayload] = {
    val userAuthToken: String                  = request.headers.get(HeaderNames.AUTHORIZATION).get
    val headerData: JsObject                   = new JsObject(request.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ",")))
    val base64EncodedDmsSubmissionHtml: String = optBase64EncodedDmsSubmissionHtml.getOrElse(
      throw new IllegalStateException("Base64 encoded DMS submission HTML not found in registration data")
    )
    val html = new String(Base64.getDecoder.decode(base64EncodedDmsSubmissionHtml))
    val pdf = buildPdf(html)
    Files.write(
      Paths.get("/Users/patricklucas/dms.pdf"),
      pdf.toByteArray
    )
    Future.successful(CreateEclSubscriptionResponsePayload(Instant.now(), ""))
  }
}
