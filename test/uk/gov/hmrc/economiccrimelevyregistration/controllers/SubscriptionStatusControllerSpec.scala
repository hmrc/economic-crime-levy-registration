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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.services.IntegrationFrameworkService

import scala.concurrent.Future

class SubscriptionStatusControllerSpec extends SpecBase {

  val mockIntegrationFrameworkService: IntegrationFrameworkService = mock[IntegrationFrameworkService]

  val controller = new SubscriptionStatusController(
    cc,
    mockIntegrationFrameworkService,
    fakeAuthorisedAction
  )

  "getSubscriptionStatus" should {
    "return 200 OK with the subscription status for a given business partner ID" in forAll {
      (businessPartnerId: String, eclSubscriptionStatus: EclSubscriptionStatus) =>
        when(mockIntegrationFrameworkService.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any()))
          .thenReturn(Future.successful(eclSubscriptionStatus))

        val result: Future[Result] =
          controller.getSubscriptionStatus(businessPartnerId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)
    }
  }

}
