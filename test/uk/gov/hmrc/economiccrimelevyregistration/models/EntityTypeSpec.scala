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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary

class EntityTypeSpec extends SpecBase {
  "writes" should {
    "return the entity type serialized to its JSON representation" in forAll { (entityType: EntityType) =>
      val result = Json.toJson(entityType)

      result shouldBe JsString(entityType.toString)
    }
  }

  "reads" should {
    "return the entity type deserialized from its JSON representation" in forAll { (entityType: EntityType) =>
      val json = Json.toJson(entityType)

      json.as[EntityType] shouldBe entityType
    }

    "return a '... is not a valid EntityType' error when passed an invalid string value" in forAll { (value: String) =>
      val result = Json.fromJson[EntityType](JsString(value))

      result shouldBe JsError(s"$value is not a valid EntityType")
    }

    "raise an error when passed a type that is not a string" in {
      val result = Json.fromJson[EntityType](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
