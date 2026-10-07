/*
 * Copyright 2026 HM Revenue & Customs
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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, GetSubscriptionResponse, HipGetSubscriptionResponse, Subscription, SubscriptionStatusResponse}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipSubscriptionConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with SubscriptionConnector {

  private def hipHeaders(correlationId: String): Seq[(String, String)] =
    Seq(
      HeaderNames.ACCEPT      -> MimeTypes.JSON,
      "correlationid"         -> correlationId,
      "X-Originating-System"  -> "ECL",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
      "X-Transmitting-System" -> "HIP",
      "Authorization"         -> s"Basic ${appConfig.hipAuthorizationToken}"
    )

  private def correlationId: String = UUID.randomUUID().toString

  // TODO cross-regime API#1534 to be implemented in tranche 7
  override def getSubscriptionStatus(idType: String, idValue: String)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionStatusResponse] = ???

  override def subscribeToEcl(safeId: String, subscription: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[CreateEclSubscriptionResponse] =
    retryFor[CreateEclSubscriptionResponse]("Subscribe to ECL")(retryCondition) {
      httpClient
        .post(
          url"${appConfig.hipBaseUrl}/etmp/RESTAdaptor/economic-crime-levy/subscription/$safeId"
        )
        .withBody(Json.toJson(subscription))
        .setHeader(hipHeaders(correlationId) *)
        .executeAndDeserialise[CreateEclSubscriptionResponse]
    }

  override def getSubscription(eclReference: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] =
    retryFor[GetSubscriptionResponse]("Get subscription")(retryCondition) {
      httpClient
        .get(url"${appConfig.hipBaseUrl}/etmp/RESTAdaptor/economic-crime-levy/subscription/$eclReference")
        .setHeader(
          hipHeaders(correlationId) *
        )
        .executeAndDeserialise[HipGetSubscriptionResponse]
        .map(_.success)
    }
}
