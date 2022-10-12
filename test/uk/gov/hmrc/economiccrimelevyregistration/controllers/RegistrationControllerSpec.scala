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

import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase {

  val mockRegistrationRepository: RegistrationRepository = mock[RegistrationRepository]

  val controller = new RegistrationController(
    cc,
    mockRegistrationRepository,
    fakeAuthorisedAction
  )

  "upsertRegistration" should {
    "return 200 OK with the registration that was upserted" in forAll { registration: Registration =>
      when(mockRegistrationRepository.upsert(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.upsertRegistration()(
          fakeRequestWithJsonBody(Json.toJson(registration))
        )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(registration)
    }
  }

  "getRegistration" should {
    "return 200 OK with an existing registration when there is one for the id" in forAll { registration: Registration =>
      when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(Some(registration)))

      val result: Future[Result] =
        controller.getRegistration(registration.internalId)(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(registration)
    }

    "return 404 NOT_FOUND when there is no registration for the id" in {
      when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(None))

      val result: Future[Result] =
        controller.getRegistration("id")(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "Registration not found"))
    }
  }

  "deleteRegistration" should {
    "return 204 NO_CONTENT when a registration is deleted" in {
      when(mockRegistrationRepository.clear(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.deleteRegistration("id")(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }
  }

}
