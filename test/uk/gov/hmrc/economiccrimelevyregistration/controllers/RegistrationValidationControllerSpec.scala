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

import cats.implicits._
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError.DataInvalid
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataValidationError, DataValidationErrors}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EclSubscription
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.economiccrimelevyregistration.services.RegistrationValidationService

import scala.concurrent.Future

class RegistrationValidationControllerSpec extends SpecBase {

  val mockRegistrationValidationService: RegistrationValidationService = mock[RegistrationValidationService]
  val mockRegistrationRepository: RegistrationRepository               = mock[RegistrationRepository]

  val controller = new RegistrationValidationController(
    cc,
    mockRegistrationRepository,
    fakeAuthorisedAction,
    mockRegistrationValidationService
  )

  "getValidationErrors" should {
    "return 204 NO_CONTENT when the registration data is valid" in forAll {
      (registration: Registration, eclSubscription: EclSubscription) =>
        when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(Some(registration)))

        when(mockRegistrationValidationService.validateRegistration(any())).thenReturn(eclSubscription.validNel)

        val result: Future[Result] =
          controller.getValidationErrors(registration.internalId)(fakeRequest)

        status(result) shouldBe NO_CONTENT
    }

    "return 200 OK with validation errors in the JSON response body when the registration data is invalid" in forAll {
      registration: Registration =>
        when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(Some(registration)))

        when(mockRegistrationValidationService.validateRegistration(any()))
          .thenReturn(DataValidationError(DataInvalid, "Invalid data").invalidNel)

        val result: Future[Result] =
          controller.getValidationErrors(registration.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(
          DataValidationErrors(Seq(DataValidationError(DataInvalid, "Invalid data")))
        )
    }

    "return 404 NOT_FOUND when there is no registration data to validate" in forAll { registration: Registration =>
      when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(None))

      val result: Future[Result] =
        controller.getValidationErrors(registration.internalId)(fakeRequest)

      status(result) shouldBe NOT_FOUND
    }
  }

}
