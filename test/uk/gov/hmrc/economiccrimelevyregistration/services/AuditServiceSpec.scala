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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val service = new AuditService(mockAuditConnector)

  "successfulSubscriptionAndEnrolment" should {
    "send a successful subscription and enrolment event to the audit connector and get an audit result back" in forAll {
      (registration: Registration, eclReference: String) =>
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result = await(service.successfulSubscriptionAndEnrolment(registration, eclReference, None))

        result shouldBe AuditResult.Success

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }
  }

  "failedSubscription" should {
    "send a failed subscription event to the audit connector and get an audit result back" in forAll {
      (registration: Registration, failureReason: String) =>
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result = await(service.failedSubscription(registration, failureReason, None))

        result shouldBe AuditResult.Success

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }
  }

  "successfulSubscriptionFailedEnrolment" should {
    "send a failed subscription event to the audit connector and get an audit result back" in forAll {
      (registration: Registration, eclReference: String, failureReason: String) =>
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result =
          await(service.successfulSubscriptionFailedEnrolment(registration, eclReference, failureReason, None))

        result shouldBe AuditResult.Success

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }
  }

}
