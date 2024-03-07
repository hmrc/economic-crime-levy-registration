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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{RegistrationError, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService

import scala.concurrent.Future

class DeregistrationControllerSpec extends SpecBase {

  val mockDeregistrationService: DeregistrationService = mock[DeregistrationService]

  val controller = new DeregistrationController(
    cc,
    mockDeregistrationService,
    fakeAuthorisedAction
  )

  "upsertDeregistration" should {
    "return 204 NO_CONTENT" in forAll { deregistration: Deregistration =>
      when(mockDeregistrationService.upsertDeregistration(ArgumentMatchers.eq(deregistration))(any()))
        .thenReturn(EitherT.rightT(deregistration))

      val result: Future[Result] =
        controller.upsertDeregistration()(
          fakeRequestWithJsonBody(Json.toJson(deregistration))
        )

      status(result) shouldBe NO_CONTENT
    }
  }

  "getDeregistration" should {
    "return 200 OK with an existing deregistration when there is one for the id" in forAll {
      deregistration: Deregistration =>
        when(mockDeregistrationService.getDeregistration(any())(any())).thenReturn(EitherT.rightT(deregistration))

        val result: Future[Result] =
          controller.getDeregistration(deregistration.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(deregistration)
    }

    "return 404 NOT_FOUND when there is no registration for the id" in {
      val eclReference: String = "XMECL001"

      when(mockDeregistrationService.getDeregistration(any())(any()))
        .thenReturn(EitherT.leftT(RegistrationError.NotFound(eclReference)))

      val result: Future[Result] =
        controller.getDeregistration("id")(fakeRequest)

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: $eclReference")
      )
    }
  }

  "deleteDeregistration" should {
    "return 204 NO_CONTENT when a deregistration is deleted" in {
      when(mockDeregistrationService.deleteDeregistration(any())(any())).thenReturn(EitherT.rightT(()))

      val result: Future[Result] =
        controller.deleteDeregistration("id")(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }
  }

}
