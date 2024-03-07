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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.RegistrationError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.economiccrimelevyregistration.services._
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService

import scala.concurrent.Future

class DeregistrationSubmissionControllerSpec extends SpecBase {

  val mockDeregistrationService: DeregistrationService = mock[DeregistrationService]
  val mockDmsService: DmsService                       = mock[DmsService]

  val controller = new DeregistrationSubmissionController(
    cc,
    mockDeregistrationService,
    fakeAuthorisedAction,
    mockDmsService
  )

  "submitRegistration" should {
    "return 200 OK when deregistration submission is successful" in forAll(
      Arbitrary.arbitrary[Deregistration],
      Arbitrary.arbitrary[CreateEclSubscriptionResponsePayload]
    ) {
      (
        deregistration: Deregistration,
        subscriptionResponse: CreateEclSubscriptionResponsePayload
      ) =>
        when(mockDeregistrationService.getDeregistration(any())(any())).thenReturn(EitherT.rightT(deregistration))

        when(mockDmsService.submitToDms(any(), any(), any())(any())).thenReturn(EitherT.rightT(subscriptionResponse))

        val result: Future[Result] =
          controller.submitDeregistration(deregistration.internalId)(fakeRequest)

        status(result) shouldBe OK

    }

    "return 404 NOT_FOUND when there is no deregistration data to submit" in forAll { registration: Registration =>
      when(mockDeregistrationService.getDeregistration(any())(any()))
        .thenReturn(EitherT.leftT(RegistrationError.NotFound(registration.internalId)))

      val result: Future[Result] =
        controller.submitDeregistration(registration.internalId)(fakeRequest)

      status(result) shouldBe NOT_FOUND
    }
  }
}
