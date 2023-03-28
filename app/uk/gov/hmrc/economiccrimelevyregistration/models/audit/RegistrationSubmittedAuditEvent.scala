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

import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

case class RegistrationResult(status: String, eclReference: Option[String], failureReason: Option[String])

object RegistrationResult {
  implicit val format: OFormat[RegistrationResult] = Json.format[RegistrationResult]
}

case class EnrolmentResult(status: String, failureReason: Option[String])

object EnrolmentResult {
  implicit val format: OFormat[EnrolmentResult] = Json.format[EnrolmentResult]
}

case class RegistrationSubmittedAuditEvent(
  registrationData: Registration,
  submissionResult: RegistrationResult,
  enrolmentResult: Option[EnrolmentResult]
) extends AuditEvent {
  override val auditType: String   = "RegistrationSubmitted"
  override val detailJson: JsValue = Json.toJson(this)
}

object RegistrationSubmittedAuditEvent {
  val SuccessStatus = "SUCCESS"
  val FailedStatus  = "FAILED"

  implicit val format: OFormat[RegistrationSubmittedAuditEvent] = Json.format[RegistrationSubmittedAuditEvent]
}
