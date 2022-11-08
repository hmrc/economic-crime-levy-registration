/*
 * Copyright 2022 HM Revenue & Customs
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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.HeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.SubscriptionStatusResponse
import uk.gov.hmrc.economiccrimelevyregistration.utils.CorrelationIdGenerator
import uk.gov.hmrc.http.HttpClient

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
          (HeaderNames.AUTHORIZATION, appConfig.integrationFrameworkBearerToken),
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

}
