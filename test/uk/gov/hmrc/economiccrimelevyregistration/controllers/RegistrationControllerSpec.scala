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
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{RegistrationError, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.services.RegistrationService

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase {

  val mockRegistrationService: RegistrationService = mock[RegistrationService]

  val controller = new RegistrationController(
    cc,
    mockRegistrationService,
    fakeAuthorisedAction
  )

  "upsertRegistration" should {
    "return 200 OK with the registration that was upserted" in forAll { registration: Registration =>
      when(mockRegistrationService.upsertRegistration(ArgumentMatchers.eq(registration))(any()))
        .thenReturn(EitherT.rightT(registration))

      val result: Future[Result] =
        controller.upsertRegistration()(
          fakeRequestWithJsonBody(Json.toJson(registration))
        )

      status(result) shouldBe OK
    }
  }

  "getRegistration" should {
    "return 200 OK with an existing registration when there is one for the id" in forAll { registration: Registration =>
      when(mockRegistrationService.getRegistration(any())(any())).thenReturn(EitherT.rightT(registration))

      val result: Future[Result] =
        controller.getRegistration(registration.internalId)(fakeRequest)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(registration)
    }

    "return 404 NOT_FOUND when there is no registration for the id" in {
      val eclReference: String = "XMECL001"

      when(mockRegistrationService.getRegistration(any())(any()))
        .thenReturn(EitherT.leftT(RegistrationError.NotFound(eclReference)))

      val result: Future[Result] =
        controller.getRegistration("id")(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: $eclReference")
      )
    }
  }

  "deleteRegistration" should {
    "return 200 OK when a registration is deleted" in {
      when(mockRegistrationService.deleteRegistration(any())(any())).thenReturn(EitherT.rightT(()))

      val result: Future[Result] =
        controller.deleteRegistration("id")(fakeRequest)

      status(result) shouldBe OK
    }
  }

}
