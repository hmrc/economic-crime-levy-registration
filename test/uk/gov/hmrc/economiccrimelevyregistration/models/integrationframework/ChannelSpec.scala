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

package uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary

class ChannelSpec extends SpecBase {

  "writes" should {
    "return the channel serialized to its JSON representation" in forAll(
      Table(
        ("channel", "expectedResult"),
        (Online, "Online"),
        (Offline, "Offline")
      )
    ) { (channel: Channel, expectedResult: String) =>
      val result = Json.toJson(channel)

      result shouldBe JsString(expectedResult)
    }
  }

  "reads" should {
    "return the channel deserialized from its JSON representation" in forAll { (channel: Channel) =>
      val json = Json.toJson(channel)

      json.as[Channel] shouldBe channel
    }

    "return a '... is not a valid Channel' error when passed an invalid string value" in forAll { (value: String) =>
      val result = Json.fromJson[Channel](JsString(value))

      result shouldBe JsError(s"$value is not a valid Channel")
    }

    "raise an error when passed a type that is not a string" in {
      val result = Json.fromJson[Channel](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
