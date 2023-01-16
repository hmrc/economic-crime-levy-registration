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

import play.api.http.HeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, SubscriptionStatusResponse}
import uk.gov.hmrc.economiccrimelevyregistration.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient,
  correlationIdGenerator: CorrelationIdGenerator
)(implicit ec: ExecutionContext) {

  def integrationFrameworkHeaders: Seq[(String, String)] = Seq(
    (HeaderNames.AUTHORIZATION, appConfig.integrationFrameworkBearerToken),
    (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
    (CustomHeaderNames.CorrelationId, correlationIdGenerator.generateCorrelationId)
  )

  def getSubscriptionStatus(
    businessPartnerId: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionStatusResponse] =
    httpClient.GET[SubscriptionStatusResponse](
      s"${appConfig.integrationFrameworkUrl}/cross-regime/subscription/ECL/SAFE/$businessPartnerId/status",
      headers = integrationFrameworkHeaders
    )

  def subscribeToEcl(businessPartnerId: String)(implicit
    hc: HeaderCarrier
  ): Future[CreateEclSubscriptionResponse] =
    Future.successful(CreateEclSubscriptionResponse(Instant.now(), "XMECL0000000001"))

}
