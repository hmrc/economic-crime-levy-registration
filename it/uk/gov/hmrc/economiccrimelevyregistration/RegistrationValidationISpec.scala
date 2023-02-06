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
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataValidationError, DataValidationErrors}

class RegistrationValidationISpec extends ISpecBase {

  s"GET ${routes.RegistrationValidationController.getValidationErrors(":id").url}" should {
    "return 204 NO_CONTENT when the registration data is valid" in {
      stubAuthorised()

      val validRegistration = random[ValidRegistration]

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      ).futureValue

      lazy val validationResult =
        callRoute(
          FakeRequest(
            routes.RegistrationValidationController.getValidationErrors(validRegistration.registration.internalId)
          )
        )

      status(validationResult) shouldBe NO_CONTENT
    }

    "return 200 OK with validation errors in the JSON response body when the registration data is invalid" in {
      stubAuthorised()

      val internalId = random[String]

      val invalidRegistration = Registration.empty(internalId)

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(Json.toJson(invalidRegistration))
      ).futureValue

      lazy val validationResult =
        callRoute(FakeRequest(routes.RegistrationValidationController.getValidationErrors(internalId)))

      val expectedErrors = Seq(
        DataValidationError("Carried out AML regulated activity choice is missing"),
        DataValidationError("Relevant AP 12 months choice is missing"),
        DataValidationError("Relevant AP revenue is missing"),
        DataValidationError("Revenue meets threshold flag is missing"),
        DataValidationError("AML supervisor is missing"),
        DataValidationError("Business sector is missing"),
        DataValidationError("First contact name is missing"),
        DataValidationError("First contact role is missing"),
        DataValidationError("First contact email is missing"),
        DataValidationError("First contact number is missing"),
        DataValidationError("Contact address is missing"),
        DataValidationError("Entity type is missing"),
        DataValidationError("Second contact choice is missing")
      )

      status(validationResult)                                      shouldBe OK
      contentAsJson(validationResult).as[DataValidationErrors].errors should contain allElementsOf expectedErrors
    }

    "return 404 NOT_FOUND when there is no registration data to validate" in {
      stubAuthorised()

      val internalId = random[String]

      lazy val validationResult =
        callRoute(FakeRequest(routes.RegistrationValidationController.getValidationErrors(internalId)))

      status(validationResult) shouldBe NOT_FOUND
    }
  }

}
