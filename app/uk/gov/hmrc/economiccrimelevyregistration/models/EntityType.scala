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

import play.api.libs.json._

sealed trait EntityType

case object UkLimitedCompany extends EntityType
case object SoleTrader extends EntityType
case object GeneralPartnership extends EntityType
case object ScottishPartnership extends EntityType
case object LimitedPartnership extends EntityType
case object ScottishLimitedPartnership extends EntityType
case object LimitedLiabilityPartnership extends EntityType

object EntityType {
  implicit val format: Format[EntityType] = new Format[EntityType] {
    override def reads(json: JsValue): JsResult[EntityType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "UkLimitedCompany"            => JsSuccess(UkLimitedCompany)
          case "SoleTrader"                  => JsSuccess(SoleTrader)
          case "GeneralPartnership"          => JsSuccess(GeneralPartnership)
          case "ScottishPartnership"         => JsSuccess(ScottishPartnership)
          case "LimitedPartnership"          => JsSuccess(LimitedPartnership)
          case "ScottishLimitedPartnership"  => JsSuccess(ScottishLimitedPartnership)
          case "LimitedLiabilityPartnership" => JsSuccess(LimitedLiabilityPartnership)
          case s                             => JsError(s"$s is not a valid EntityType")
        }
      case e: JsError          => e
    }

    override def writes(o: EntityType): JsValue = o match {
      case UkLimitedCompany            => JsString(UkLimitedCompany.toString)
      case SoleTrader                  => JsString(SoleTrader.toString)
      case GeneralPartnership          => JsString(GeneralPartnership.toString)
      case ScottishPartnership         => JsString(ScottishPartnership.toString)
      case LimitedPartnership          => JsString(LimitedPartnership.toString)
      case ScottishLimitedPartnership  => JsString(ScottishLimitedPartnership.toString)
      case LimitedLiabilityPartnership => JsString(LimitedLiabilityPartnership.toString)
    }
  }
}
