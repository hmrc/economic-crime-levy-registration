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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}
import play.api.mvc.JavascriptLiteral

sealed abstract class RegistrationType(value: String)

object RegistrationType {

  private val amendment      = "Amendment"
  private val deRegistration = "DeRegistration"
  private val initial        = "Initial"

  case object Initial extends RegistrationType(initial)
  case object Amendment extends RegistrationType(amendment)
  case object DeRegistration extends RegistrationType(deRegistration)

  lazy val values: Set[RegistrationType] = Set(Initial, Amendment, DeRegistration)

  implicit val format: Format[RegistrationType] = new Format[RegistrationType] {
    override def reads(json: JsValue): JsResult[RegistrationType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Amendment"      => JsSuccess(Amendment)
          case "DeRegistration" => JsSuccess(DeRegistration)
          case "Initial"        => JsSuccess(Initial)
        }
      case e: JsError          => e
    }

    override def writes(o: RegistrationType): JsValue = o match {
      case Amendment      => JsString(amendment)
      case DeRegistration => JsString(deRegistration)
      case Initial        => JsString(initial)
    }
  }

  implicit val jsLiteral: JavascriptLiteral[RegistrationType] = new JavascriptLiteral[RegistrationType] {
    override def to(value: RegistrationType): String = value match {
      case Amendment      => amendment
      case DeRegistration => deRegistration
      case Initial        => initial
    }
  }
}
