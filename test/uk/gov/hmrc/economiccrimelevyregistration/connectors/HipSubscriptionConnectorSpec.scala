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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.*
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription, GetSubscriptionResponse, HipGetSubscriptionResponse}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class HipSubscriptionConnectorSpec extends SpecBase {

  val actorSystem: ActorSystem = ActorSystem("test")
  val config: Config           = app.injector.instanceOf[Config]

  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val connector                    = new HipSubscriptionConnector(appConfig, mockHttpClient, config, actorSystem)

  "getSubscription" should {
    "successfully return a subscription from the success wrapper when using the hip connector " in forAll {
      (eclReference: String, correlationId: String, getSubscriptionResponse: HipGetSubscriptionResponse) =>
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(
            Future.successful(HttpResponse.apply(ACCEPTED, Json.toJson(getSubscriptionResponse).toString()))
          )

        val result = await(connector.getSubscription(eclReference))

        result shouldBe getSubscriptionResponse.success
    }
  }
}
