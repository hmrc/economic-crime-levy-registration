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

  private val deRegistered: String    = "DeRegistered"
  private val eclRegReference: String = "eclRegistrationReference"
  private val status: String          = "status"
  private val subscribed: String      = "Subscribed"
  private val notValidStatus: String  = "is not a valid SubscriptionStatus"

  case class DeRegistered(eclRegistrationReference: String) extends SubscriptionStatus

  case object NotSubscribed extends SubscriptionStatus

  case class Subscribed(eclRegistrationReference: String) extends SubscriptionStatus

  implicit val subscriptionStatusFormat: Format[SubscriptionStatus] = new Format[SubscriptionStatus] {
    override def reads(json: JsValue): JsResult[SubscriptionStatus] = json match {
      case JsString(value) =>
        value match {
          case "NotSubscribed" => JsSuccess(NotSubscribed)
          case s               => JsError(s"$s $notValidStatus")
        }
      case json            =>
        (json \ status, json \ eclRegReference) match {
          case (JsDefined(status), JsDefined(eclRegistrationReference)) =>
            (status.as[String], eclRegistrationReference.as[String]) match {
              case ("Subscribed", eclRegistrationReference)   => JsSuccess(Subscribed(eclRegistrationReference))
              case ("DeRegistered", eclRegistrationReference) => JsSuccess(DeRegistered(eclRegistrationReference))
              case (s, _)                                     => JsError(s"$s $notValidStatus")
            }
          case _                                                        => JsError(s"$json $notValidStatus")
        }
    }

    override def writes(o: SubscriptionStatus): JsValue = o match {
      case DeRegistered(eclRegistrationReference) =>
        Json.obj(status -> deRegistered, eclRegReference -> eclRegistrationReference)
      case Subscribed(eclRegistrationReference)   =>
        Json.obj(status -> subscribed, eclRegReference -> eclRegistrationReference)
      case subscriptionStatus                     => JsString(subscriptionStatus.toString)
    }
  }

  implicit val format: OFormat[EclSubscriptionStatus] = Json.format[EclSubscriptionStatus]
}
