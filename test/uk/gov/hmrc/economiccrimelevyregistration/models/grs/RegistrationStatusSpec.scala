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

package uk.gov.hmrc.economiccrimelevyregistration.models.grs

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._

class RegistrationStatusSpec extends SpecBase {
  "writes" should {
    "return the registration status serialized to its JSON representation" in forAll(
      Table(
        ("registrationStatus", "expectedResult"),
        (Registered, "REGISTERED"),
        (RegistrationFailed, "REGISTRATION_FAILED"),
        (RegistrationNotCalled, "REGISTRATION_NOT_CALLED")
      )
    ) { (registrationStatus: RegistrationStatus, expectedResult: String) =>
      val result = Json.toJson(registrationStatus)

      result shouldBe JsString(expectedResult)
    }
  }

  "reads" should {
    "return the registration status deserialized from its JSON representation" in forAll {
      (registrationStatus: RegistrationStatus) =>
        val json = Json.toJson(registrationStatus)

        json.as[RegistrationStatus] shouldBe registrationStatus
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[RegistrationStatus](JsString("Test"))

      result shouldBe JsError("Test is not a valid RegistrationStatus")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[RegistrationStatus](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
