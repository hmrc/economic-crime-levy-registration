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
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.Future

class RegistrationsControllerSpec extends SpecBase {

  val mockRegistrationRepository: RegistrationRepository = mock[RegistrationRepository]

  val controller = new RegistrationController(
    cc,
    mockRegistrationRepository,
    fakeAuthorisedAction
  )

  "createRegistration" should {
    "create a new registration and return OK with the registration that was created" in {
      when(mockRegistrationRepository.upsert(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.createRegistration()(fakeRequestWithJsonBody(Json.toJson(emptyRegistration)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyRegistration)
    }
  }

  "getRegistration" should {
    "return an existing registration when there is one for the id" in {
      when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(Some(emptyRegistration)))

      val result: Future[Result] =
        controller.getRegistration("id")(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(emptyRegistration)
    }

    "return 404 not found when there is no registration for the id" in {
      when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(None))

      val result: Future[Result] =
        controller.getRegistration("id")(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse(NOT_FOUND, "Registration not found"))
    }
  }

  "updateRegistration" should {
    "???" in {}
  }

  "deleteRegistration" should {
    "???" in {}
  }

}
