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

package uk.gov.hmrc.economiccrimelevyregistration.services

import cats.data.Validated.Valid
import uk.gov.hmrc.economiccrimelevyregistration.ValidRegistration
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError

class RegistrationValidationServiceSpec extends SpecBase {
  val service = new RegistrationValidationService()

  "validateRegistration" should {
    "return the registration if it is valid" in forAll { validRegistration: ValidRegistration =>
      val result = service.validateRegistration(validRegistration.registration)

      result shouldBe Valid(validRegistration.registration)
    }

    "return a non-empty chain of errors when unconditional mandatory registration data items are missing" in {
      val registration = Registration.empty("internalId")

      val expectedErrors = Seq(
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

      val result = service.validateRegistration(registration)

      result.isValid shouldBe false
      result.leftMap(nec => nec.toNonEmptyList.toList should contain allElementsOf expectedErrors)
    }

    "return an error if the entity type is uk limited company but there is incorporated entity data in the registration" in {
      pending
    }

    "return an error if the entity type is partnership but there is no partnership data in the registration" in {
      pending
    }

    "return an error if the entity type is sole trader but there is no sole trader data in the registration" in {
      pending
    }

    "return an error if the registration data does not contain the business partner ID" in {
      pending
    }

    "return errors if the second contact choice is true and there are no second contact details in the registration data" in {
      pending
    }
  }
}
