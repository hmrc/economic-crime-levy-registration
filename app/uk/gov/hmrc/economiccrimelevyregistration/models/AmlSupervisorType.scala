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

import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.Other

sealed trait AmlSupervisorType

object AmlSupervisorType {
  case object Hmrc extends AmlSupervisorType
  case object GamblingCommission extends AmlSupervisorType
  case object FinancialConductAuthority extends AmlSupervisorType
  case object Other extends AmlSupervisorType
  case object Unknown extends AmlSupervisorType

  implicit val format: Format[AmlSupervisorType] = new Format[AmlSupervisorType] {
    override def reads(json: JsValue): JsResult[AmlSupervisorType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Hmrc"                      => JsSuccess(Hmrc)
          case "GamblingCommission"        => JsSuccess(GamblingCommission)
          case "FinancialConductAuthority" => JsSuccess(FinancialConductAuthority)
          case "Other"                     => JsSuccess(Other)
          case "Unknown"                   => JsSuccess(Unknown)
          case s                           => JsError(s"$s is not a valid AmlSupervisor")
        }
      case e: JsError          => e
    }

    override def writes(o: AmlSupervisorType): JsValue = JsString(o.toString)
  }
}

final case class AmlSupervisor(supervisorType: AmlSupervisorType, otherProfessionalBody: Option[String]) {
  val professionalBody: String =
    supervisorType match {
      case Other => otherProfessionalBody.map(_.toString).getOrElse("Unknown")
      case value => value.toString
    }
}

object AmlSupervisor {
  implicit val format: OFormat[AmlSupervisor] = Json.format[AmlSupervisor]
}
