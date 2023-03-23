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

import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, UpsertKnownFactsRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait EnrolmentStoreProxyConnector {
  def upsertKnownFacts(upsertKnownFactsRequest: UpsertKnownFactsRequest, eclReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]]
}

@Singleton
class EnrolmentStoreProxyConnectorImpl @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends EnrolmentStoreProxyConnector {

  private val enrolmentStoreUrl: String =
    s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  def upsertKnownFacts(upsertKnownFactsRequest: UpsertKnownFactsRequest, eclReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val enrolmentKey = s"${EclEnrolment.ServiceName}~${EclEnrolment.IdentifierKey}~$eclReference"

    httpClient.PUT[UpsertKnownFactsRequest, Either[UpstreamErrorResponse, HttpResponse]](
      s"$enrolmentStoreUrl/enrolments/$enrolmentKey",
      upsertKnownFactsRequest
    )
  }
}
