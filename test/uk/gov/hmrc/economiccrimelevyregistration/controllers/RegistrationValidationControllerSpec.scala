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
import cats.implicits._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.ValidScottishOrGeneralPartnershipRegistration
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationAdditionalInfo
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, DataValidationError}
import uk.gov.hmrc.economiccrimelevyregistration.services.{RegistrationAdditionalInfoService, RegistrationService, RegistrationValidationService}

import scala.concurrent.Future

class RegistrationValidationControllerSpec extends SpecBase {

  val mockRegistrationValidationService: RegistrationValidationService         = mock[RegistrationValidationService]
  val mockRegistrationService: RegistrationService                             = mock[RegistrationService]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]

  val controller = new RegistrationValidationController(
    cc,
    mockRegistrationService,
    fakeAuthorisedAction,
    mockRegistrationValidationService,
    mockRegistrationAdditionalInfoService
  )

  "getValidationErrors" should {
    "return 200 OK when the registration data is valid" in forAll {
      (
        registration: ValidScottishOrGeneralPartnershipRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        when(mockRegistrationService.getRegistration(any())(any()))
          .thenReturn(EitherT.rightT(registration.registration))

        when(
          mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.registration.internalId))(any())
        )
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

        when(mockRegistrationValidationService.validateSubscription(any()))
          .thenReturn(EitherT.rightT(registration.expectedEclSubscription))

        val result: Future[Result] =
          controller.checkForValidationErrors(registration.registration.internalId)(fakeRequest)

        status(result) shouldBe OK
    }

    "return 200 OK with DataValidationError in the JSON response body when the registration data is invalid" in forAll {
      (
        registration: ValidScottishOrGeneralPartnershipRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        when(mockRegistrationService.getRegistration(any())(any()))
          .thenReturn(EitherT.rightT(registration.registration))

        when(
          mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.registration.internalId))(any())
        )
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

        when(mockRegistrationValidationService.validateSubscription(any()))
          .thenReturn(EitherT.leftT(DataValidationError.DataInvalid("Invalid data")))

        val result: Future[Result] =
          controller.checkForValidationErrors(registration.registration.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson("Invalid data")
    }
  }

}
