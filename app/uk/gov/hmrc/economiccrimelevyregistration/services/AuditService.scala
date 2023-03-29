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

package uk.gov.hmrc.economiccrimelevyregistration.services

import play.api.Logging
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{EnrolmentResult, RegistrationResult, RegistrationSubmittedAuditEvent, RequestStatus}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuditService @Inject() (
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def successfulSubscriptionAndEnrolment(
    registrationData: Registration,
    eclReference: String
  ): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      RegistrationSubmittedAuditEvent(
        registrationData = registrationData,
        submissionResult = RegistrationResult(RequestStatus.Success, Some(eclReference), None),
        enrolmentResult = Some(EnrolmentResult(RequestStatus.Success, None))
      ).extendedDataEvent
    )

  def failedSubscription(
    registrationData: Registration,
    failureReason: String
  ): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      RegistrationSubmittedAuditEvent(
        registrationData = registrationData,
        submissionResult = RegistrationResult(RequestStatus.Failed, None, Some(failureReason)),
        enrolmentResult = None
      ).extendedDataEvent
    )

  def successfulSubscriptionFailedEnrolment(
    registrationData: Registration,
    eclReference: String,
    enrolmentFailureReason: String
  ): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      RegistrationSubmittedAuditEvent(
        registrationData = registrationData,
        submissionResult = RegistrationResult(RequestStatus.Success, Some(eclReference), None),
        enrolmentResult = Some(EnrolmentResult(RequestStatus.Failed, Some(enrolmentFailureReason)))
      ).extendedDataEvent
    )

}
