/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.models.deregister

import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
class DeregisterReasonSpec extends SpecBase {

  "reads" should {
    "return error when trying to deserialize unknown DeregisterReason" in forAll {
      (deregisterReason: DeregisterReason) =>
        val json  = Json.toJson("Unknown")
        val error = intercept[JsResultException] {
          json.as[DeregisterReason] shouldBe deregisterReason
        }
        assert(error.errors.nonEmpty)
    }

    "return error when trying to deserialize wrong data type" in forAll { (deregisterReason: DeregisterReason) =>
      val json = Json.toJson(1)

      val error = intercept[JsResultException] {
        json.as[DeregisterReason] shouldBe deregisterReason
      }
      assert(error.errors.nonEmpty)
    }
  }
}
