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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.HeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription, Subscription, SubscriptionStatusResponse}
import uk.gov.hmrc.economiccrimelevyregistration.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.{HttpClient, UpstreamErrorResponse}

import scala.concurrent.Future

class IntegrationFrameworkConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient                         = mock[HttpClient]
  val mockCorrelationIdGenerator: CorrelationIdGenerator = mock[CorrelationIdGenerator]
  val connector                                          = new IntegrationFrameworkConnector(appConfig, mockHttpClient, mockCorrelationIdGenerator)

  "getSubscriptionStatus" should {
    "return a subscription status when the http client returns a subscription status" in forAll {
      (
        businessPartnerId: String,
        subscriptionStatusResponse: SubscriptionStatusResponse,
        correlationId: String
      ) =>
        val expectedUrl =
          s"${appConfig.integrationFrameworkUrl}/cross-regime/subscription/ECL/SAFE/$businessPartnerId/status"

        val expectedHeaders: Seq[(String, String)] = Seq(
          (HeaderNames.AUTHORIZATION, s"Bearer ${appConfig.integrationFrameworkBearerToken}"),
          (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
          (CustomHeaderNames.CorrelationId, correlationId)
        )

        when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

        when(
          mockHttpClient.GET[SubscriptionStatusResponse](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any())
        )
          .thenReturn(Future.successful(subscriptionStatusResponse))

        val result = await(connector.getSubscriptionStatus(businessPartnerId))

        result shouldBe subscriptionStatusResponse

        verify(mockHttpClient, times(1))
          .GET[SubscriptionStatusResponse](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }

  "subscribeToEcl" should {
    "return either an error or create subscription response when the http client returns one" in forAll {
      (
        eclSubscription: EclSubscription,
        eitherResult: Either[UpstreamErrorResponse, CreateEclSubscriptionResponse],
        correlationId: String
      ) =>
        val expectedUrl =
          s"${appConfig.integrationFrameworkUrl}/economic-crime-levy/subscription/${eclSubscription.businessPartnerId}"

        val expectedHeaders: Seq[(String, String)] = Seq(
          (HeaderNames.AUTHORIZATION, s"Bearer ${appConfig.integrationFrameworkBearerToken}"),
          (CustomHeaderNames.Environment, appConfig.integrationFrameworkEnvironment),
          (CustomHeaderNames.CorrelationId, correlationId)
        )

        when(mockCorrelationIdGenerator.generateCorrelationId).thenReturn(correlationId)

        when(
          mockHttpClient.POST[Subscription, Either[UpstreamErrorResponse, CreateEclSubscriptionResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(eclSubscription.subscription),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(eitherResult))

        val result = await(connector.subscribeToEcl(eclSubscription))

        result shouldBe eitherResult

        verify(mockHttpClient, times(1))
          .POST[Subscription, Either[UpstreamErrorResponse, CreateEclSubscriptionResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(eclSubscription.subscription),
            ArgumentMatchers.eq(expectedHeaders)
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }
  }

}
