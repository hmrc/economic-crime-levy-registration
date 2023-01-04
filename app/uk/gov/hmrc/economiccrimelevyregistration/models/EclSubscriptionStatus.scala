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

sealed trait SubscriptionStatus

final case class EclSubscriptionStatus(subscriptionStatus: SubscriptionStatus)

object EclSubscriptionStatus {
  case class Subscribed(eclRegistrationReference: String) extends SubscriptionStatus
  case object NotSubscribed extends SubscriptionStatus

  implicit val subscriptionStatusFormat: Format[SubscriptionStatus] = new Format[SubscriptionStatus] {
    override def reads(json: JsValue): JsResult[SubscriptionStatus] = json match {
      case JsString(value) =>
        value match {
          case "NotSubscribed" => JsSuccess(NotSubscribed)
          case s               => JsError(s"$s is not a valid SubscriptionStatus")
        }
      case json            =>
        (json \ "status", json \ "eclRegistrationReference") match {
          case (JsDefined(status), JsDefined(eclRegistrationReference)) =>
            (status.as[String], eclRegistrationReference.as[String]) match {
              case ("Subscribed", eclRegistrationReference) => JsSuccess(Subscribed(eclRegistrationReference))
              case (s, _)                                   => JsError(s"$s is not a valid SubscriptionStatus")
            }
          case _                                                        => JsError(s"$json is not a valid SubscriptionStatus")
        }
    }

    override def writes(o: SubscriptionStatus): JsValue = o match {
      case Subscribed(eclRegistrationReference) =>
        Json.obj("status" -> "Subscribed", "eclRegistrationReference" -> eclRegistrationReference)
      case subscriptionStatus                   => JsString(subscriptionStatus.toString)
    }
  }

  implicit val format: OFormat[EclSubscriptionStatus] = Json.format[EclSubscriptionStatus]
}
