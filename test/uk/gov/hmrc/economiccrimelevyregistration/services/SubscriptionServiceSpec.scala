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
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SubscriptionSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription}
import uk.gov.hmrc.economiccrimelevyregistration.models.{KnownFactsWorkItem, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongo.workitem.WorkItem
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import javax.management.RuntimeErrorException
import scala.concurrent.Future

class SubscriptionServiceSpec extends SpecBase {

  val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector               = mock[TaxEnrolmentsConnector]
  val mockKnownFactsQueueRepository: KnownFactsQueueRepository         = mock[KnownFactsQueueRepository]
  val mockAuditService: AuditService                                   = mock[AuditService]
  val mockAuditConnector: AuditConnector                               = mock[AuditConnector]
  val errorMessage                                                     = "Error message"
  val upstreamErrorResponse: UpstreamErrorResponse                     = UpstreamErrorResponse(errorMessage, INTERNAL_SERVER_ERROR)
  private val dateFormatter                                            = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

  val service = new SubscriptionService(
    mockIntegrationFrameworkConnector,
    mockTaxEnrolmentsConnector,
    mockKnownFactsQueueRepository,
    mockAuditService,
    mockAuditConnector
  )

  "subscribe to ECL" should {
    "return a successful Future and correct response if Integration Framework call is successful" in forAll {
      (
        eclSubscription: EclSubscription,
        subscriptionResponse: CreateEclSubscriptionResponse,
        registration: Registration,
        liabilityYear: Int
      ) =>
        reset(mockIntegrationFrameworkConnector)
        reset(mockAuditService)

        when(mockIntegrationFrameworkConnector.subscribeToEcl(any(), any())(any()))
          .thenReturn(Future.successful(subscriptionResponse))

        val result =
          await(service.executeCallToIntegrationFramework(eclSubscription, registration, Some(liabilityYear)).value)

        result shouldBe Right(subscriptionResponse)

        verify(mockIntegrationFrameworkConnector, times(1))
          .subscribeToEcl(any(), any())(any())

        verify(mockAuditService, times(0))
          .failedSubscription(any(), any(), any())(any())
    }

    "return successful Future with SubscriptionSubmissionError if Integration framework call failed" in forAll {
      (
        eclSubscription: EclSubscription,
        registration: Registration,
        liabilityYear: Int
      ) =>
        reset(mockIntegrationFrameworkConnector)
        reset(mockAuditService)

        when(mockIntegrationFrameworkConnector.subscribeToEcl(any(), any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, INTERNAL_SERVER_ERROR)))

        when(mockAuditService.failedSubscription(any(), any(), any())(any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result =
          await(service.executeCallToIntegrationFramework(eclSubscription, registration, Some(liabilityYear)).value)

        result shouldBe Left(SubscriptionSubmissionError.BadGateway(errorMessage, INTERNAL_SERVER_ERROR))

        verify(mockIntegrationFrameworkConnector, times(1))
          .subscribeToEcl(any(), any())(any())

        verify(mockAuditService, times(1))
          .failedSubscription(any(), any(), any())(any())
    }

    "return successful Future without any payload if Tax enrolment call is successful" in forAll {
      (
        enrolmentRequest: CreateEnrolmentRequest,
        registration: Registration,
        eclReference: String,
        liabilityYear: Int,
        dateProcessed: Instant
      ) =>
        reset(mockTaxEnrolmentsConnector)
        reset(mockAuditService)

        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.successful(()))

        when(mockAuditService.successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result = await(
          service
            .executeCallToTaxEnrolment(enrolmentRequest, registration, eclReference, Some(liabilityYear), dateProcessed)
            .value
        )

        result shouldBe Right(())

        verify(mockTaxEnrolmentsConnector, times(1))
          .enrol(any())(any())

        verify(mockAuditService, times(0))
          .successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any())
    }

    "return successful Future with SubscriptionSubmissionError if Tax enrolment call failed" in forAll {
      (
        enrolmentRequest: CreateEnrolmentRequest,
        registration: Registration,
        eclReference: String,
        liabilityYear: Int,
        dateProcessed: Instant,
        workItem: WorkItem[KnownFactsWorkItem]
      ) =>
        reset(mockTaxEnrolmentsConnector)
        reset(mockAuditService)
        reset(mockKnownFactsQueueRepository)

        val knownFactsWorkItem = KnownFactsWorkItem(eclReference, dateFormatter.format(dateProcessed))

        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, INTERNAL_SERVER_ERROR)))

        when(mockKnownFactsQueueRepository.pushNew(ArgumentMatchers.eq(knownFactsWorkItem), any(), any()))
          .thenReturn(Future.successful(workItem))

        when(mockAuditService.successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result = await(
          service
            .executeCallToTaxEnrolment(enrolmentRequest, registration, eclReference, Some(liabilityYear), dateProcessed)
            .value
        )

        result shouldBe Left(SubscriptionSubmissionError.BadGateway(errorMessage, INTERNAL_SERVER_ERROR))

        verify(mockTaxEnrolmentsConnector, times(1))
          .enrol(any())(any())

        verify(mockAuditService, times(1))
          .successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any())
    }
  }

  "return successful Future without payload if call to KnownFactsQueueRepository is successful" in forAll {
    (workItem: WorkItem[KnownFactsWorkItem]) =>
      reset(mockKnownFactsQueueRepository)

      val knownFactsWorkItem = KnownFactsWorkItem(testEclRegistrationReference, dateFormatter.format(Instant.now()))

      when(mockKnownFactsQueueRepository.pushNew(ArgumentMatchers.eq(knownFactsWorkItem), any(), any()))
        .thenReturn(Future.successful(workItem))

      val result = await(service.executeCallToKnownFactsQueueRepository(knownFactsWorkItem).value)

      result shouldBe Right(())

      verify(mockKnownFactsQueueRepository, times(1))
        .pushNew(ArgumentMatchers.eq(knownFactsWorkItem), any(), any())
  }

  "return successful Future with SubscriptionSubmissionError if call to KnownFactsQueueRepository has failed" in forAll {
    (knownFactsWorkItem: KnownFactsWorkItem) =>
      reset(mockKnownFactsQueueRepository)

      val exception = new RuntimeErrorException(new Error(), errorMessage)

      when(mockKnownFactsQueueRepository.pushNew(ArgumentMatchers.eq(knownFactsWorkItem), any(), any()))
        .thenReturn(Future.failed(exception))

      val result = await(service.executeCallToKnownFactsQueueRepository(knownFactsWorkItem).value)

      result shouldBe Left(SubscriptionSubmissionError.InternalUnexpectedError(errorMessage, Some(exception)))

      verify(mockKnownFactsQueueRepository, times(1))
        .pushNew(ArgumentMatchers.eq(knownFactsWorkItem), any(), any())
  }

  "return successful Future with CreateEclSubscriptionResponse payload if calls to all services that methods depends on are successful" in forAll {
    (
      subscriptionResponse: CreateEclSubscriptionResponse,
      eclSubscription: EclSubscription,
      registration: Registration,
      liabilityYear: Int
    ) =>
      reset(mockAuditService)
      reset(mockTaxEnrolmentsConnector)
      reset(mockIntegrationFrameworkConnector)

      when(mockIntegrationFrameworkConnector.subscribeToEcl(any(), any())(any()))
        .thenReturn(Future.successful(subscriptionResponse))

      when(mockTaxEnrolmentsConnector.enrol(any())(any()))
        .thenReturn(Future.successful(()))

      when(mockAuditService.successfulSubscriptionAndEnrolment(any(), any(), any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val result = await(service.subscribeToEcl(eclSubscription, registration, Some(liabilityYear)).value)

      result shouldBe Right(subscriptionResponse)

      verify(mockAuditService, times(1))
        .successfulSubscriptionAndEnrolment(any(), any(), any())(any())

      verify(mockTaxEnrolmentsConnector, times(1))
        .enrol(any())(any())

      verify(mockAuditService, times(1))
        .successfulSubscriptionAndEnrolment(any(), any(), any())(any())
  }

  "return successful Future with SubscriptionSubmissionError payload if call to one of the services that method depends on fails" in forAll {
    (
      subscriptionResponse: CreateEclSubscriptionResponse,
      eclSubscription: EclSubscription,
      registration: Registration,
      liabilityYear: Int
    ) =>
      reset(mockAuditService)
      reset(mockTaxEnrolmentsConnector)
      reset(mockIntegrationFrameworkConnector)

      val exception = new RuntimeErrorException(new Error(), errorMessage)

      when(mockIntegrationFrameworkConnector.subscribeToEcl(any(), any())(any()))
        .thenReturn(Future.successful(subscriptionResponse))

      when(mockTaxEnrolmentsConnector.enrol(any())(any()))
        .thenReturn(Future.successful(()))

      when(mockAuditService.successfulSubscriptionAndEnrolment(any(), any(), any())(any()))
        .thenReturn(Future.successful(AuditResult.Failure(errorMessage, Some(exception))))

      val result = await(service.subscribeToEcl(eclSubscription, registration, Some(liabilityYear)).value)

      result shouldBe Right(subscriptionResponse)

      verify(mockAuditService, times(1))
        .successfulSubscriptionAndEnrolment(any(), any(), any())(any())

      verify(mockTaxEnrolmentsConnector, times(1))
        .enrol(any())(any())

      verify(mockAuditService, times(1))
        .successfulSubscriptionAndEnrolment(any(), any(), any())(any())
  }
  "executeCallToIntegrationFrameworkForSubscriptionStatus" should {
    "return Left - SubscriptionSubmissionError.BadGateway with 5xx code when call to integrationFrameworkConnector fails" in {
      val code       = BAD_GATEWAY
      val internalId = UUID.randomUUID().toString
      when(mockIntegrationFrameworkConnector.getSubscriptionStatus(any(), any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply(errorMessage, code)))

      val result =
        await(service.getSubscriptionStatus("id-type", "id-value", internalId).value)

      result shouldBe Left(SubscriptionSubmissionError.BadGateway(reason = errorMessage, code = code))
    }
  }

  "executeCallToIntegrationFrameworkForSubscriptionStatus" should {
    "return Left - SubscriptionSubmissionError.BadGateway with 4xx code when call to integrationFrameworkConnector fails" in {
      val code       = BAD_REQUEST
      val internalId = UUID.randomUUID().toString
      when(mockIntegrationFrameworkConnector.getSubscriptionStatus(any(), any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse.apply(errorMessage, code)))

      val result =
        await(service.getSubscriptionStatus("id-type", "id-value", internalId).value)

      result shouldBe Left(SubscriptionSubmissionError.BadGateway(reason = errorMessage, code = code))
    }
  }

  "executeCallToIntegrationFrameworkForSubscriptionStatus" should {
    "return Left - SubscriptionSubmissionError.InternalUnexpectedError when call to integrationFrameworkConnector fails" in {
      val internalId = UUID.randomUUID().toString
      val exception  = new Exception(errorMessage)

      when(mockIntegrationFrameworkConnector.getSubscriptionStatus(any(), any())(any()))
        .thenReturn(Future.failed(exception))

      val result =
        await(service.getSubscriptionStatus("id-type", "id-value", internalId).value)

      result shouldBe Left(SubscriptionSubmissionError.InternalUnexpectedError(errorMessage, Some(exception)))
    }
  }

  "executeCallToTaxEnrolment"         should {
    "return Left - SubscriptionSubmissionError.BadGateway when call to taxEnrolmentsConnector fails with 4xx error" in forAll {
      (
        workItem: WorkItem[KnownFactsWorkItem],
        auditResult: AuditResult,
        enrolmentRequest: CreateEnrolmentRequest,
        registration: Registration
      ) =>
        val code                   = BAD_REQUEST
        val dateProcessed: Instant = Instant.now()

        val knownFactsWorkItem = KnownFactsWorkItem(testEclRegistrationReference, dateFormatter.format(dateProcessed))

        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply(errorMessage, code)))

        when(mockKnownFactsQueueRepository.pushNew(ArgumentMatchers.eq(knownFactsWorkItem), any(), any()))
          .thenReturn(Future.successful(workItem))

        when(mockAuditService.successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(auditResult))

        val result = await(
          service
            .executeCallToTaxEnrolment(
              enrolmentRequest,
              registration,
              testEclRegistrationReference,
              Some(LocalDate.now().getYear),
              Instant.now()
            )
            .value
        )
        result shouldBe Left(SubscriptionSubmissionError.BadGateway(reason = errorMessage, code = code))
    }

    "return Left - SubscriptionSubmissionError.InternalUnexpectedError when call to taxEnrolmentsConnector fails with NonFatal error" in forAll {
      (
        workItem: WorkItem[KnownFactsWorkItem],
        auditResult: AuditResult,
        enrolmentRequest: CreateEnrolmentRequest,
        registration: Registration
      ) =>
        val exception              = new Exception(errorMessage)
        val dateProcessed: Instant = Instant.now()

        val knownFactsWorkItem = KnownFactsWorkItem(testEclRegistrationReference, dateFormatter.format(dateProcessed))

        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.failed(exception))

        when(mockKnownFactsQueueRepository.pushNew(ArgumentMatchers.eq(knownFactsWorkItem), any(), any()))
          .thenReturn(Future.successful(workItem))

        when(mockAuditService.successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(auditResult))

        val result = await(
          service
            .executeCallToTaxEnrolment(
              enrolmentRequest,
              registration,
              testEclRegistrationReference,
              Some(LocalDate.now().getYear),
              Instant.now()
            )
            .value
        )

        result shouldBe Left(SubscriptionSubmissionError.InternalUnexpectedError(errorMessage, Some(exception)))
    }
  }
  "executeCallToIntegrationFramework" should {
    "return Left - SubscriptionSubmissionError.BadGateway when call to integrationFrameworkConnector fails with 4xx error" in forAll {
      (auditResult: AuditResult, eclSubscription: EclSubscription, registration: Registration, liabilityYear: Int) =>
        when(
          mockIntegrationFrameworkConnector
            .subscribeToEcl(any(), any())(any())
        ).thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, BAD_REQUEST)))

        when(mockAuditService.successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(auditResult))

        val result =
          await(service.executeCallToIntegrationFramework(eclSubscription, registration, Some(liabilityYear)).value)

        result shouldBe Left(SubscriptionSubmissionError.BadGateway(reason = errorMessage, code = BAD_REQUEST))
    }

    "return Left - SubscriptionSubmissionError.InternalUnexpectedError when call to integrationFrameworkConnector fails with NonFatal error" in forAll {
      (auditResult: AuditResult, eclSubscription: EclSubscription, registration: Registration, liabilityYear: Int) =>
        val exception = new Exception(errorMessage)

        when(
          mockIntegrationFrameworkConnector
            .subscribeToEcl(any(), any())(any())
        ).thenReturn(Future.failed(exception))

        when(mockAuditService.successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(auditResult))

        val result =
          await(service.executeCallToIntegrationFramework(eclSubscription, registration, Some(liabilityYear)).value)

        result shouldBe Left(SubscriptionSubmissionError.InternalUnexpectedError(errorMessage, Some(exception)))
    }
  }
}
