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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.services.SubscriptionService
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.GetSubscriptionResponse

import scala.concurrent.Future

class SubscriptionControllerSpec extends SpecBase {

  val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]
  val mockSubscriptionService: SubscriptionService                     = mock[SubscriptionService]
  val controller                                                       = new SubscriptionController(
    cc,
    mockSubscriptionService,
    fakeAuthorisedAction
  )

  "getSubscriptionStatus" should {
    "return 200 OK with the subscription status for a given business partner ID" in forAll {
      (businessPartnerId: String, subscriptionStatus: EclSubscriptionStatus) =>
        when(mockSubscriptionService.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId), any())(any()))
          .thenReturn(EitherT.rightT(subscriptionStatus))

        val result: Future[Result] =
          controller.getSubscriptionStatus(businessPartnerId)(fakeRequest)

        status(result) shouldBe OK

        contentAsJson(result) shouldBe Json.toJson(subscriptionStatus)

        reset(mockIntegrationFrameworkConnector)
    }
  }

  "getSubscription" should {
    "return 200 OK with subscription data for given ecl reference" in forAll {
      (eclReference: String, getSubscriptionResponse: GetSubscriptionResponse) =>
        when(mockSubscriptionService.getSubscription(any())(any()))
          .thenReturn(Future.successful(getSubscriptionResponse))

        val result: Future[Result] = controller.getSubscription(eclReference)(fakeRequest)

        status(result) shouldBe OK

        contentAsJson(result) shouldBe Json.toJson(getSubscriptionResponse)

        verify(mockSubscriptionService, times(1)).getSubscription(ArgumentMatchers.eq(eclReference))(any())

        reset(mockIntegrationFrameworkConnector)
        reset(mockSubscriptionService)
    }

    "return 500 InternalServerError when call to IF fails" in forAll { (eclReference: String) =>
      when(mockSubscriptionService.getSubscription(ArgumentMatchers.eq(eclReference))(any()))
        .thenReturn(Future.failed(new IllegalStateException("Error")))

      val result = intercept[IllegalStateException] {
        await(
          controller
            .getSubscription(eclReference)(fakeRequest)
        )
      }

      result.getMessage shouldBe "Error"
      result            shouldBe a[IllegalStateException]

      reset(mockIntegrationFrameworkConnector)
      reset(mockSubscriptionService)
    }
  }

}
