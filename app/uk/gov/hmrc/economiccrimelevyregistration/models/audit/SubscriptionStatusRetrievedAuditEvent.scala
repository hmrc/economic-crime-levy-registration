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

package uk.gov.hmrc.economiccrimelevyregistration.models.audit

import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{Channel, EtmpSubscriptionStatus}

case class AuditSubscriptionStatus(
  subscriptionStatus: EtmpSubscriptionStatus,
  eclReference: Option[String],
  channel: Option[Channel]
)

object AuditSubscriptionStatus {
  implicit val writes: OWrites[AuditSubscriptionStatus] = Json.writes[AuditSubscriptionStatus]
}

case class SubscriptionStatusRetrievedAuditEvent(
  internalId: String,
  idType: String,
  idValue: String,
  subscriptionStatus: AuditSubscriptionStatus
) extends AuditEvent {
  override val auditType: String   = "SubscriptionStatusRetrieved"
  override val detailJson: JsValue = Json.toJson(this)
}

object SubscriptionStatusRetrievedAuditEvent {
  implicit val writes: OWrites[SubscriptionStatusRetrievedAuditEvent] =
    Json.writes[SubscriptionStatusRetrievedAuditEvent]
}
