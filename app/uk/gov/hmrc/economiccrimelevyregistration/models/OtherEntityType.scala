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

sealed trait OtherEntityType

object OtherEntityType {
  case object Charity extends OtherEntityType
  case object Trust extends OtherEntityType
  case object RegisteredSociety extends OtherEntityType
  case object NonUKEstablishment extends OtherEntityType
  case object UnincorporatedAssociation extends OtherEntityType

  implicit val format: Format[OtherEntityType] = new Format[OtherEntityType] {
    override def reads(json: JsValue): JsResult[OtherEntityType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Charity"                   => JsSuccess(Charity)
          case "Trust"                     => JsSuccess(Trust)
          case "RegisteredSociety"         => JsSuccess(RegisteredSociety)
          case "NonUKEstablishment"        => JsSuccess(NonUKEstablishment)
          case "UnincorporatedAssociation" => JsSuccess(UnincorporatedAssociation)
          case s                           => JsError(s"$s is not a valid OtherEntityType")
        }
      case e: JsError          => e
    }

    override def writes(o: OtherEntityType): JsValue = JsString(o.toString)
  }
}
