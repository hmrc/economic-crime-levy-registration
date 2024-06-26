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

import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EtmpSubscriptionStatus.ContractObjectInactive

sealed trait EtmpSubscriptionStatus

object EtmpSubscriptionStatus {
  case object ApprovedWithConditions extends EtmpSubscriptionStatus
  case object ContractObjectInactive extends EtmpSubscriptionStatus
  case object CreateFailed extends EtmpSubscriptionStatus
  case object DeRegistered extends EtmpSubscriptionStatus
  case object DsOutcomeInProgress extends EtmpSubscriptionStatus
  case object InProcessing extends EtmpSubscriptionStatus
  case object NoFormBundleFound extends EtmpSubscriptionStatus
  case object RegFormReceived extends EtmpSubscriptionStatus
  case object Rejected extends EtmpSubscriptionStatus
  case object Revoked extends EtmpSubscriptionStatus
  case object SentToDs extends EtmpSubscriptionStatus
  case object SentToRcm extends EtmpSubscriptionStatus
  case object Successful extends EtmpSubscriptionStatus
  case object Withdrawal extends EtmpSubscriptionStatus

  implicit val reads: Reads[EtmpSubscriptionStatus] = (json: JsValue) =>
    json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "APPROVED_WITH_CONDITIONS" => JsSuccess(ApprovedWithConditions)
          case "CONTRACT_OBJECT_INACTIVE" => JsSuccess(ContractObjectInactive)
          case "CREATE_FAILED"            => JsSuccess(CreateFailed)
          case "DE-REGISTERED"            => JsSuccess(DeRegistered)
          case "DS_OUTCOME_IN_PROGRESS"   => JsSuccess(DsOutcomeInProgress)
          case "IN_PROCESSING"            => JsSuccess(InProcessing)
          case "NO_FORM_BUNDLE_FOUND"     => JsSuccess(NoFormBundleFound)
          case "REG_FORM_RECEIVED"        => JsSuccess(RegFormReceived)
          case "REJECTED"                 => JsSuccess(Rejected)
          case "REVOKED"                  => JsSuccess(Revoked)
          case "SENT_TO_DS"               => JsSuccess(SentToDs)
          case "SENT_TO_RCM"              => JsSuccess(SentToRcm)
          case "SUCCESSFUL"               => JsSuccess(Successful)
          case "WITHDRAWAL"               => JsSuccess(Withdrawal)
          case s                          => JsError(s"$s is not a valid SubscriptionStatus")
        }
      case e: JsError          => e
    }

  implicit val writes: Writes[EtmpSubscriptionStatus] = (o: EtmpSubscriptionStatus) => JsString(o.toString)
}

sealed trait Channel

object Channel {
  case object Online extends Channel
  case object Offline extends Channel

  implicit val reads: Reads[Channel] = (json: JsValue) =>
    json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Online"  => JsSuccess(Online)
          case "Offline" => JsSuccess(Offline)
          case s         => JsError(s"$s is not a valid Channel")
        }
      case e: JsError          => e
    }

  implicit val writes: Writes[Channel] = (o: Channel) => JsString(o.toString)
}

final case class SubscriptionStatusResponse(
  subscriptionStatus: EtmpSubscriptionStatus,
  idType: Option[String],
  idValue: Option[String],
  channel: Option[Channel]
) {
  def toEclSubscriptionStatus: EclSubscriptionStatus =
    (
      subscriptionStatus,
      idType,
      idValue
    ) match {
      case (ContractObjectInactive, Some("ZECL"), Some(eclRegistrationReference)) =>
        EclSubscriptionStatus(DeRegistered(eclRegistrationReference))
      case (_, Some("ZECL"), Some(eclRegistrationReference))                      =>
        EclSubscriptionStatus(Subscribed(eclRegistrationReference))
      case _                                                                      => EclSubscriptionStatus(NotSubscribed)
    }
}

object SubscriptionStatusResponse {
  implicit val reads: Reads[SubscriptionStatusResponse]   = Json.reads[SubscriptionStatusResponse]
  implicit val writes: Writes[SubscriptionStatusResponse] = (o: SubscriptionStatusResponse) => JsString(o.toString)

  implicit val format: OFormat[SubscriptionStatusResponse] = Json.format
}
