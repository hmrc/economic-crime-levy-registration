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

package uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._

class ChannelSpec extends SpecBase {
  "reads" should {
    "return the channel deserialized from its JSON representation" in forAll { channel: Channel =>
      val json = JsString(channel.toString)

      json.as[Channel] shouldBe channel
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[Channel](JsString("Test"))

      result shouldBe JsError(s"Test is not a valid Channel")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[Channel](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }

  "writes" should {
    "return the channel serialized to its JSON representation" in forAll { channel: Channel =>
      Json.toJson(channel) shouldBe JsString(channel.toString)
    }
  }
}
