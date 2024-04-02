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

sealed trait UtrType

object UtrType {
  case object CtUtr extends UtrType

  case object SaUtr extends UtrType

  implicit val format: Format[UtrType] = new Format[UtrType] {
    override def reads(json: JsValue): JsResult[UtrType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "CtUtr" => JsSuccess(CtUtr)
          case "SaUtr" => JsSuccess(SaUtr)
          case s       => JsError(s"$s is not a valid UtrType")
        }
      case e: JsError          => e
    }

    override def writes(o: UtrType): JsValue = JsString(o.toString)
  }
}
