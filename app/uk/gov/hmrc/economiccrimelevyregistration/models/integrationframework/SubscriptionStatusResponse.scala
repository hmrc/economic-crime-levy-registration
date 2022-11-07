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

package uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, OFormat}

sealed trait SubscriptionStatus

case object NoFormBundleFound extends SubscriptionStatus
case object RegFormReceived extends SubscriptionStatus
case object SentToDs extends SubscriptionStatus
case object DsOutcomeInProgress extends SubscriptionStatus
case object Successful extends SubscriptionStatus
case object Rejected extends SubscriptionStatus
case object InProcessing extends SubscriptionStatus
case object CreateFailed extends SubscriptionStatus
case object Withdrawal extends SubscriptionStatus
case object SentToRcm extends SubscriptionStatus
case object ApprovedWithConditions extends SubscriptionStatus
case object Revoked extends SubscriptionStatus
case object DeRegistered extends SubscriptionStatus
case object ContractObjectInactive extends SubscriptionStatus

object SubscriptionStatus {

  implicit val format: Format[SubscriptionStatus] = new Format[SubscriptionStatus] {
    override def reads(json: JsValue): JsResult[SubscriptionStatus] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "NO_FORM_BUNDLE_FOUND"     => JsSuccess(NoFormBundleFound)
          case "REG_FORM_RECEIVED"        => JsSuccess(RegFormReceived)
          case "SENT_TO_DS"               => JsSuccess(SentToDs)
          case "DS_OUTCOME_IN_PROGRESS"   => JsSuccess(DsOutcomeInProgress)
          case "SUCCESSFUL"               => JsSuccess(Successful)
          case "REJECTED"                 => JsSuccess(Rejected)
          case "IN_PROCESSING"            => JsSuccess(InProcessing)
          case "CREATE_FAILED"            => JsSuccess(CreateFailed)
          case "WITHDRAWAL"               => JsSuccess(Withdrawal)
          case "SENT_TO_RCM"              => JsSuccess(SentToRcm)
          case "APPROVED_WITH_CONDITIONS" => JsSuccess(ApprovedWithConditions)
          case "REVOKED"                  => JsSuccess(Revoked)
          case "DE-REGISTERED"            => JsSuccess(DeRegistered)
          case "CONTRACT_OBJECT_INACTIVE" => JsSuccess(ContractObjectInactive)
          case s                          => JsError(s"$s is not a valid SubscriptionStatus")
        }
      case e: JsError          => e
    }

    override def writes(o: SubscriptionStatus): JsValue = o match {
      case NoFormBundleFound      => JsString("NO_FORM_BUNDLE_FOUND")
      case RegFormReceived        => JsString("REG_FORM_RECEIVED")
      case SentToDs               => JsString("SENT_TO_DS")
      case DsOutcomeInProgress    => JsString("DS_OUTCOME_IN_PROGRESS")
      case Successful             => JsString("SUCCESSFUL")
      case Rejected               => JsString("REJECTED")
      case InProcessing           => JsString("IN_PROCESSING")
      case CreateFailed           => JsString("CREATE_FAILED")
      case Withdrawal             => JsString("WITHDRAWAL")
      case SentToRcm              => JsString("SENT_TO_RCM")
      case ApprovedWithConditions => JsString("APPROVED_WITH_CONDITIONS")
      case Revoked                => JsString("REVOKED")
      case DeRegistered           => JsString("DE-REGISTERED")
      case ContractObjectInactive => JsString("CONTRACT_OBJECT_INACTIVE")
    }
  }
}

sealed trait Channel

case object Online extends Channel
case object Offline extends Channel

object Channel {
  implicit val format: Format[Channel] = new Format[Channel] {
    override def reads(json: JsValue): JsResult[Channel] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Online"  => JsSuccess(Online)
          case "Offline" => JsSuccess(Offline)
          case s         => JsError(s"$s is not a valid Channel")
        }
      case e: JsError          => e
    }

    override def writes(o: Channel): JsValue = o match {
      case Online  => JsString(Online.toString)
      case Offline => JsString(Offline.toString)
    }
  }
}

final case class SubscriptionStatusResponse(
  subscriptionStatus: SubscriptionStatus,
  idType: String,
  idValue: String,
  channel: Channel
)

object SubscriptionStatusResponse {
  implicit val format: OFormat[SubscriptionStatusResponse] = Json.format[SubscriptionStatusResponse]
}
