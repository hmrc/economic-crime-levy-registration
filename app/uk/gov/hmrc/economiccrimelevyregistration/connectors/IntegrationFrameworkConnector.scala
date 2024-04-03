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

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, StringContextOps}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BaseConnector {

  private def createCorrelationId(hc: HeaderCarrier): String =
    hc.headers(Seq(CustomHeaderNames.xCorrelationId)) match {
      case Seq((_, correlationId)) => correlationId
      case _                       => UUID.randomUUID().toString
    }

  private def integrationFrameworkHeaders(correlationId: String, bearerToken: String): Seq[(String, String)] = Seq(
    (HeaderNames.authorisation, s"Bearer $bearerToken"),
    (CustomHeaderNames.environment, appConfig.integrationFrameworkEnvironment),
    (CustomHeaderNames.correlationId, correlationId),
    (CustomHeaderNames.xCorrelationId, correlationId)
  )

  def getSubscriptionStatus(
    idType: String,
    idValue: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionStatusResponse] = {
    val correlationId = createCorrelationId(hc)

    retryFor[SubscriptionStatusResponse]("Get subscription status")(retryCondition) {
      httpClient
        .get(url"${appConfig.integrationFrameworkUrl}/cross-regime/subscription/ECL/$idType/$idValue/status")
        .setHeader(integrationFrameworkHeaders(correlationId, appConfig.getSubscriptionStatusBearerToken): _*)
        .executeAndDeserialise[SubscriptionStatusResponse]
    }
  }

  def subscribeToEcl(businessPartnerId: String, subscription: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[CreateEclSubscriptionResponse] = {
    val correlationId: String = createCorrelationId(hc)

    retryFor[CreateEclSubscriptionResponse]("Subscribe to ECL")(retryCondition) {
      httpClient
        .post(
          url"${appConfig.integrationFrameworkUrl}/economic-crime-levy/subscription/$businessPartnerId"
        )
        .withBody(Json.toJson(subscription))
        .setHeader(integrationFrameworkHeaders(correlationId, appConfig.integrationFrameworkBearerToken): _*)
        .executeAndDeserialise[CreateEclSubscriptionResponse]
    }
  }

  def getSubscription(
    eclReference: String
  )(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] = {
    val correlationId: String = createCorrelationId(hc)

    retryFor[GetSubscriptionResponse]("Get subscription")(retryCondition) {
      httpClient
        .get(url"${appConfig.integrationFrameworkUrl}/economic-crime-levy/subscription/$eclReference")
        .setHeader(
          integrationFrameworkHeaders(correlationId, appConfig.integrationFrameworkGetSubscriptionBearerToken): _*
        )
        .executeAndDeserialise[GetSubscriptionResponse]
    }
  }

}
