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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IntegrationFrameworkConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{KnownFactsWorkItem, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import scala.concurrent.Future

class SubscriptionServiceSpec extends SpecBase {
  val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector               = mock[TaxEnrolmentsConnector]
  val mockKnownFactsQueueRepository: KnownFactsQueueRepository         = mock[KnownFactsQueueRepository]
  val mockAuditService: AuditService                                   = mock[AuditService]

  val service = new SubscriptionService(
    mockIntegrationFrameworkConnector,
    mockTaxEnrolmentsConnector,
    mockKnownFactsQueueRepository,
    mockAuditService
  )

  "subscribeToEcl" should {
    "return the ECL reference number when the subscription and enrolment is successful" in forAll {
      (
        registration: Registration,
        eclSubscription: EclSubscription,
        subscriptionResponse: CreateEclSubscriptionResponse
      ) =>
        when(mockIntegrationFrameworkConnector.subscribeToEcl(ArgumentMatchers.eq(eclSubscription))(any()))
          .thenReturn(Future.successful(Right(subscriptionResponse)))

        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.successful(Right(HttpResponse(OK, "", Map.empty))))

        val result = await(service.subscribeToEcl(eclSubscription, registration))

        result shouldBe subscriptionResponse

        verify(mockAuditService, times(1)).successfulSubscriptionAndEnrolment(
          ArgumentMatchers.eq(registration),
          ArgumentMatchers.eq(subscriptionResponse.eclReference)
        )

        reset(mockAuditService)
    }

    "return the ECL reference number and push the known facts to a queue when the subscription is successful but the enrolment fails" in forAll {
      (
        registration: Registration,
        eclSubscription: EclSubscription,
        subscriptionResponse: CreateEclSubscriptionResponse,
        workItem: WorkItem[KnownFactsWorkItem]
      ) =>
        val updatedSubscriptionResponse =
          subscriptionResponse.copy(processingDate = Instant.parse("2007-12-25T10:15:30.00Z"))

        val error = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(mockIntegrationFrameworkConnector.subscribeToEcl(ArgumentMatchers.eq(eclSubscription))(any()))
          .thenReturn(Future.successful(Right(updatedSubscriptionResponse)))

        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.successful(Left(error)))

        val expectedKnownFactsWorkItem =
          KnownFactsWorkItem(eclReference = updatedSubscriptionResponse.eclReference, eclRegistrationDate = "20071225")

        when(mockKnownFactsQueueRepository.pushNew(ArgumentMatchers.eq(expectedKnownFactsWorkItem), any(), any()))
          .thenReturn(Future.successful(workItem.copy(item = expectedKnownFactsWorkItem)))

        val result = await(service.subscribeToEcl(eclSubscription, registration))

        result shouldBe updatedSubscriptionResponse

        verify(mockKnownFactsQueueRepository, times(1))
          .pushNew(ArgumentMatchers.eq(expectedKnownFactsWorkItem), any(), any())

        reset(mockKnownFactsQueueRepository)

        verify(mockAuditService, times(1)).successfulSubscriptionFailedEnrolment(
          ArgumentMatchers.eq(registration),
          ArgumentMatchers.eq(subscriptionResponse.eclReference),
          ArgumentMatchers.eq(error.getMessage())
        )

        reset(mockAuditService)
    }

    "throw an exception when the subscription fails" in forAll {
      (
        registration: Registration,
        eclSubscription: EclSubscription
      ) =>
        val error = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(mockIntegrationFrameworkConnector.subscribeToEcl(ArgumentMatchers.eq(eclSubscription))(any()))
          .thenReturn(Future.successful(Left(error)))

        val result = intercept[UpstreamErrorResponse] {
          await(service.subscribeToEcl(eclSubscription, registration))
        }

        result shouldBe error

        verify(mockAuditService, times(1))
          .failedSubscription(ArgumentMatchers.eq(registration), ArgumentMatchers.eq(error.getMessage()))

        reset(mockAuditService)
    }
  }
}
