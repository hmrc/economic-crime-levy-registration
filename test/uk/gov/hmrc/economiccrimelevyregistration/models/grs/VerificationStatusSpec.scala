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

package uk.gov.hmrc.economiccrimelevyregistration.models.grs

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus._

class VerificationStatusSpec extends SpecBase {
  "writes" should {
    "return the verification status serialized to its JSON representation" in forAll(
      Table(
        ("verificationStatus", "expectedResult"),
        (Pass, "PASS"),
        (Fail, "FAIL"),
        (Unchallenged, "UNCHALLENGED"),
        (CtEnrolled, "CT_ENROLLED"),
        (SaEnrolled, "SA_ENROLLED")
      )
    ) { (verificationStatus: VerificationStatus, expectedResult: String) =>
      val result = Json.toJson(verificationStatus)

      result shouldBe JsString(expectedResult)
    }
  }

  "reads" should {
    "return the verification status deserialized from its JSON representation" in forAll {
      (verificationStatus: VerificationStatus) =>
        val json = Json.toJson(verificationStatus)

        json.as[VerificationStatus] shouldBe verificationStatus
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[VerificationStatus](JsString("Test"))

      result shouldBe JsError("Test is not a valid VerificationStatus")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[VerificationStatus](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
