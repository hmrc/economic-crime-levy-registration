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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.UpsertKnownFactsRequest
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val actorSystem: ActorSystem = ActorSystem("test")
  val config: Config           = app.injector.instanceOf[Config]

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val connector                          = new EnrolmentStoreProxyConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  val enrolmentStoreUrl: String = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  "upsertKnownFacts" should {
    "return successful empty response" in forAll {
      (
        eclReference: String,
        upsertKnownFactsRequest: UpsertKnownFactsRequest
      ) =>
        when(mockHttpClient.put(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(ACCEPTED, "")))

        val result: Unit = await(connector.upsertKnownFacts(upsertKnownFactsRequest, eclReference)(hc))

        result shouldBe ()

        verify(mockRequestBuilder, times(1))
          .execute(any(), any())

        reset(mockRequestBuilder)
    }
  }

}
