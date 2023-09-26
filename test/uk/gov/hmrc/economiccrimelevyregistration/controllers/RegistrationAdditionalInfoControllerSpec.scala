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
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationAdditionalInfo
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.services.RegistrationAdditionalInfoService

import scala.concurrent.Future

class RegistrationAdditionalInfoControllerSpec extends SpecBase {

  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]

  val controller = new RegistrationAdditionalInfoController(
    cc,
    mockRegistrationAdditionalInfoService,
    fakeAuthorisedAction
  )

  "upsert" should {
    "return 200 OK when the registration additional upsert succeeds" in forAll {
      registrationAdditionalInfo: RegistrationAdditionalInfo =>
        when(mockRegistrationAdditionalInfoService.upsert(ArgumentMatchers.eq(registrationAdditionalInfo)))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](()))

        val result: Future[Result] =
          controller.upsert()(
            fakeRequestWithJsonBody(Json.toJson(registrationAdditionalInfo))
          )

        status(result) shouldBe OK
    }

    "return 500 INTERNAL_SERVER_ERROR when the registration additional upsert fails" in forAll {
      registrationAdditionalInfo: RegistrationAdditionalInfo =>
        when(mockRegistrationAdditionalInfoService.upsert(ArgumentMatchers.eq(registrationAdditionalInfo)))
          .thenReturn(
            EitherT.leftT[Future, Unit](DataRetrievalError.InternalUnexpectedError("Error", None))
          )

        val result: Future[Result] =
          controller.upsert()(
            fakeRequestWithJsonBody(Json.toJson(registrationAdditionalInfo))
          )

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "get" should {
    "return 200 OK with an existing registration when there is one for the id" in forAll {
      registrationAdditionalInfo: RegistrationAdditionalInfo =>
        when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registrationAdditionalInfo.internalId)))
          .thenReturn(
            EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo)
          )

        val result: Future[Result] =
          controller.get(registrationAdditionalInfo.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(registrationAdditionalInfo)
    }

    "return 404 NOT_FOUND when there is no registration for the id" in forAll {
      registrationAdditionalInfo: RegistrationAdditionalInfo =>
        when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registrationAdditionalInfo.internalId)))
          .thenReturn(
            EitherT.leftT[Future, RegistrationAdditionalInfo](
              DataRetrievalError.NotFound(registrationAdditionalInfo.internalId)
            )
          )

        val result: Future[Result] =
          controller.get(registrationAdditionalInfo.internalId)(fakeRequest)

        status(result) shouldBe NOT_FOUND
    }

    "return 500 INTERNAL_SERVER_ERROR when there is no registration for the id" in forAll {
      registrationAdditionalInfo: RegistrationAdditionalInfo =>
        when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registrationAdditionalInfo.internalId)))
          .thenReturn(
            EitherT.leftT[Future, RegistrationAdditionalInfo](
              DataRetrievalError.NotFound(registrationAdditionalInfo.internalId)
            )
          )

        val result: Future[Result] =
          controller.get(registrationAdditionalInfo.internalId)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "delete" should {
    "return 200 OK when deleting a registration additional info record succeeds" in forAll {
      registrationAdditionalInfo: RegistrationAdditionalInfo =>
        when(mockRegistrationAdditionalInfoService.delete(ArgumentMatchers.eq(registrationAdditionalInfo.internalId)))
          .thenReturn(EitherT.rightT[Future, DataRetrievalError](()))

        val result: Future[Result] =
          controller.delete(registrationAdditionalInfo.internalId)(fakeRequest)

        status(result) shouldBe OK
    }

    "return 500 INTERNAL_SERVER_ERROR when deleting a registration additional info fails" in forAll {
      registrationAdditionalInfo: RegistrationAdditionalInfo =>
        when(mockRegistrationAdditionalInfoService.delete(ArgumentMatchers.eq(registrationAdditionalInfo.internalId)))
          .thenReturn(
            EitherT.leftT[Future, Unit](DataRetrievalError.InternalUnexpectedError("Error", None))
          )

        val result: Future[Result] =
          controller.delete(registrationAdditionalInfo.internalId)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
