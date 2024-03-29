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

package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.ResponseError

class RegistrationISpec extends ISpecBase {

  s"PUT ${routes.RegistrationController.upsertRegistration.url}"           should {
    "create or update a registration and return 200 OK with the registration" in {
      stubAuthorised()

      val registration = random[Registration]

      lazy val putResult = callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(Json.toJson(registration))
      )

      lazy val getResult =
        callRoute(FakeRequest(routes.RegistrationController.getRegistration(registration.internalId)))

      status(putResult)        shouldBe OK
      status(getResult)        shouldBe OK
      contentAsJson(getResult) shouldBe Json.toJson(registration.copy(lastUpdated = Some(now)))
    }
  }

  s"GET ${routes.RegistrationController.getRegistration(":id").url}"       should {
    "return 200 OK with a registration that is already in the database" in {
      stubAuthorised()

      val registration = random[Registration]

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(Json.toJson(registration))
      ).futureValue

      lazy val result =
        callRoute(FakeRequest(routes.RegistrationController.getRegistration(registration.internalId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(registration.copy(lastUpdated = Some(now)))
    }

    "return 404 NOT_FOUND when trying to get a registration that doesn't exist" in {
      stubAuthorised()

      val registration      = random[Registration]
      val validRegistration = registration.copy(internalId = "internalId")

      val result = callRoute(FakeRequest(routes.RegistrationController.getRegistration(validRegistration.internalId)))

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: ${validRegistration.internalId}")
      )
    }
  }

  s"DELETE ${routes.RegistrationController.deleteRegistration(":id").url}" should {
    "delete a registration and return 200 OK" in {
      stubAuthorised()

      val registration = random[Registration]

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(Json.toJson(registration))
      ).futureValue

      lazy val getResultBeforeDelete =
        callRoute(FakeRequest(routes.RegistrationController.getRegistration(registration.internalId)))

      lazy val deleteResult =
        callRoute(FakeRequest(routes.RegistrationController.deleteRegistration(registration.internalId)))

      lazy val getResultAfterDelete =
        callRoute(FakeRequest(routes.RegistrationController.getRegistration(registration.internalId)))

      status(getResultBeforeDelete) shouldBe OK
      status(deleteResult)          shouldBe OK
      status(getResultAfterDelete)  shouldBe NOT_FOUND
    }
  }

}
