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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IntegrationFrameworkConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SubscriptionSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SubscriptionSubmissionError.NonFatalUnexpectedError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription}
import uk.gov.hmrc.economiccrimelevyregistration.models.{KnownFactsWorkItem, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongo.workitem.WorkItem
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import scala.concurrent.Future

class SubscriptionServiceSpecV2 extends SpecBase {

  val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector               = mock[TaxEnrolmentsConnector]
  val mockKnownFactsQueueRepository: KnownFactsQueueRepository         = mock[KnownFactsQueueRepository]
  val mockAuditService: AuditService                                   = mock[AuditService]
  val errorMessage                                                     = "Error message"
  val upstreamErrorResponse                                            = UpstreamErrorResponse(errorMessage, INTERNAL_SERVER_ERROR)
  private val dateFormatter                                            = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

  val service = new SubscriptionService(
    mockIntegrationFrameworkConnector,
    mockTaxEnrolmentsConnector,
    mockKnownFactsQueueRepository,
    mockAuditService
  )

  "subscribe to ECL" should {
    "return a successful Future and correct response if Integration Framework call is successful" in forAll {
      (eclSubscription: EclSubscription, subscriptionResponse: CreateEclSubscriptionResponse) =>
        when(mockIntegrationFrameworkConnector.subscribeToEcl(any())(any()))
          .thenReturn(Future.successful(subscriptionResponse))

        val result = await(service.executeCallToIntegrationFramework(eclSubscription).value)
        result shouldBe Right(subscriptionResponse)
    }

    "return successful Future with SubscriptionSubmissionError if Integration framework call failed" in forAll {
      (eclSubscription: EclSubscription, subscriptionResponse: CreateEclSubscriptionResponse) =>
        when(mockIntegrationFrameworkConnector.subscribeToEcl(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, INTERNAL_SERVER_ERROR)))

        val result = await(service.executeCallToIntegrationFramework(eclSubscription).value)

        result shouldBe Left(SubscriptionSubmissionError.BadGateway(errorMessage, INTERNAL_SERVER_ERROR))
    }

    "return successful Future without any payload if Tax enrolment call is successful" in forAll {
      (enrolmentRequest: CreateEnrolmentRequest) =>
        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.successful())

        val result = await(service.executeCallToTaxEnrolment(enrolmentRequest).value)

        result shouldBe Right(())
    }

    "return successful Future with SubscriptionSubmissionError if Tax enrolment call failed" in forAll {
      (enrolmentRequest: CreateEnrolmentRequest) =>
        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, INTERNAL_SERVER_ERROR)))

        val result = await(service.executeCallToTaxEnrolment(enrolmentRequest).value)

        result shouldBe Left(SubscriptionSubmissionError.BadGateway(errorMessage, INTERNAL_SERVER_ERROR))
    }
  }

  "return successful Future without payload if call to KnownFactsQueueRepository is successful" in forAll {
    (workItem: WorkItem[KnownFactsWorkItem]) =>
      val knownFactsWorkItem = KnownFactsWorkItem(testEclRegistrationReference, dateFormatter.format(Instant.now()))
      when(mockKnownFactsQueueRepository.pushNew(any()))
        .thenReturn(Future.successful(workItem))

      val result = await(service.executeCallToKnownFactsQueueRepository(knownFactsWorkItem).value)

      result shouldBe Right(())
  }

  "return successful Future with SubscriptionSubmissionError if call to KnownFactsQueueRepository has failed" in forAll {
    (knownFactsWorkItem: KnownFactsWorkItem) =>
      when(mockKnownFactsQueueRepository.pushNew(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, INTERNAL_SERVER_ERROR)))

      val result = await(service.executeCallToKnownFactsQueueRepository(knownFactsWorkItem).value)

      result shouldBe Left(SubscriptionSubmissionError.BadGateway(errorMessage, INTERNAL_SERVER_ERROR))
  }

//  "return successful Future without payload if call to Audit service is successful for successfulSubscriptionAndEnrolment method" in forAll {
//    (registration: Registration, eclReference: String, liabilityYear: Int) =>
//      when(mockAuditService.successfulSubscriptionAndEnrolment(any(), any(), any())(any()))
//        .thenReturn(Future.successful(AuditResult.Success))
//
//      val result = await(service.executeCallAuditSerice(registration, Some(eclReference), None, Some(liabilityYear)))
//
//      result shouldBe Right(())
//  }
//
//  "return successful Future with NonFatalUnexpectedError if call to Audit service is successful for successfulSubscriptionFailedEnrolment method" in forAll {
//    (registration: Registration, eclReference: String, liabilityYear: Int) =>
//      when(mockAuditService.successfulSubscriptionFailedEnrolment(any(), any(), any(), any())(any()))
//        .thenReturn(Future.successful(AuditResult.Success))
//
//      val result = await(
//        service
//          .executeCallAuditSerice(registration, Some(eclReference), Some(upstreamErrorResponse), Some(liabilityYear))
//      )
//
//      result shouldBe Left(
//        NonFatalUnexpectedError(
//          s"Failed enrolment for successful subscription for eclReference: $eclReference " +
//            s"with message: ${upstreamErrorResponse.message}",
//          None
//        )
//      )
//  }
//
//  "return successful Future with BadGateway error if call to Audit service is successful for failedSubscription method" in forAll {
//    (registration: Registration, eclReference: String, liabilityYear: Int) =>
//      when(mockAuditService.failedSubscription(any(), any(), any())(any()))
//        .thenReturn(Future.successful(AuditResult.Success))
//
//      val result =
//        await(service.executeCallAuditSerice(registration, None, Some(upstreamErrorResponse), Some(liabilityYear)))
//
//      result shouldBe Left(
//        SubscriptionSubmissionError.BadGateway(
//          s"Failed registration with error message " +
//            s"${upstreamErrorResponse.message}",
//          upstreamErrorResponse.statusCode
//        )
//      )
//  }
}
