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

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IntegrationFrameworkConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{AuditSubscriptionStatus, SubscriptionStatusRetrievedAuditEvent}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SubscriptionSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription, SubscriptionStatusResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, KeyValue, KnownFactsWorkItem, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.GetSubscriptionResponse
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubscriptionService @Inject() (
  integrationFrameworkConnector: IntegrationFrameworkConnector,
  taxEnrolmentsConnector: TaxEnrolmentsConnector,
  knownFactsQueueRepository: KnownFactsQueueRepository,
  auditService: AuditService,
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

  def subscribeToEcl(
    eclSubscription: EclSubscription,
    registration: Registration,
    liabilityYear: Option[Int]
  )(implicit hc: HeaderCarrier): EitherT[Future, SubscriptionSubmissionError, CreateEclSubscriptionResponse] =
    for {
      integrationFrameworkResult <- executeCallToIntegrationFramework(eclSubscription, registration, liabilityYear)
      eclReference                = integrationFrameworkResult.success.eclReference
      processingDate              = integrationFrameworkResult.success.processingDate
      _                          <- executeCallToTaxEnrolment(
                                      createEnrolmentRequest(integrationFrameworkResult),
                                      registration,
                                      eclReference,
                                      liabilityYear,
                                      processingDate
                                    )
      _                           = auditService.successfulSubscriptionAndEnrolment(registration, eclReference, liabilityYear)

    } yield integrationFrameworkResult

  def getSubscriptionStatus(businessPartnerId: String, internalId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SubscriptionSubmissionError, EclSubscriptionStatus] =
    for {
      eclSubscriptionStatus <- executeCallToIntegrationFrameworkForSubscriptionStatus(businessPartnerId, internalId)
    } yield eclSubscriptionStatus.toEclSubscriptionStatus

  private def executeCallToIntegrationFrameworkForSubscriptionStatus(businessPartnerId: String, internalId: String)(
    implicit hc: HeaderCarrier
  ): EitherT[Future, SubscriptionSubmissionError, SubscriptionStatusResponse] =
    EitherT {
      integrationFrameworkConnector
        .getSubscriptionStatus(businessPartnerId)
        .map { response =>
          executeExtendedAuditEvent(businessPartnerId, response, internalId)
          Right(response)
        }
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse
                .unapply(error)
                .isDefined =>
            Left(SubscriptionSubmissionError.BadGateway(message, code))
          case NonFatal(thr) => Left(SubscriptionSubmissionError.InternalUnexpectedError(thr.getMessage, Some(thr)))

        }
    }
  def executeCallToKnownFactsQueueRepository(
    knownFactsWorkItem: KnownFactsWorkItem
  ): EitherT[Future, SubscriptionSubmissionError, Unit]                       =
    EitherT {
      knownFactsQueueRepository
        .pushNew(knownFactsWorkItem)
        .map(_ => Right(()))
        .recover { case error =>
          Left(SubscriptionSubmissionError.InternalUnexpectedError(error.getMessage, Some(error)))
        }
    }

  def executeCallToTaxEnrolment(
    enrolmentRequest: CreateEnrolmentRequest,
    registration: Registration,
    eclReference: String,
    liabilityYear: Option[Int],
    processingDate: Instant
  )(implicit hc: HeaderCarrier): EitherT[Future, SubscriptionSubmissionError, Unit] =
    EitherT {
      taxEnrolmentsConnector
        .enrol(enrolmentRequest)
        .map(_ => Right(()))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            executeCallToKnownFactsQueueRepository(
              KnownFactsWorkItem(eclReference, dateFormatter.format(processingDate))
            )
            auditService.successfulSubscriptionFailedEnrolment(registration, eclReference, message, liabilityYear)

            Left(SubscriptionSubmissionError.BadGateway(reason = message, code = code))
          case NonFatal(thr) =>
            executeCallToKnownFactsQueueRepository(
              KnownFactsWorkItem(eclReference, dateFormatter.format(processingDate))
            )
            auditService.successfulSubscriptionFailedEnrolment(
              registration,
              eclReference,
              thr.getMessage,
              liabilityYear
            )

            Left(SubscriptionSubmissionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def executeCallToIntegrationFramework(
    eclSubscription: EclSubscription,
    registration: Registration,
    liabilityYear: Option[Int]
  )(implicit hc: HeaderCarrier): EitherT[Future, SubscriptionSubmissionError, CreateEclSubscriptionResponse] =
    EitherT {
      integrationFrameworkConnector
        .subscribeToEcl(eclSubscription)
        .map(response => Right(response))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            auditService.failedSubscription(registration, message, liabilityYear)
            Left(SubscriptionSubmissionError.BadGateway(reason = message, code = code))
          case NonFatal(thr) =>
            auditService.failedSubscription(registration, thr.getMessage, liabilityYear)
            Left(SubscriptionSubmissionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  private def createEnrolmentRequest(subscriptionResponse: CreateEclSubscriptionResponse): CreateEnrolmentRequest =
    CreateEnrolmentRequest(
      identifiers = Seq(KeyValue(IdentifierKey, subscriptionResponse.success.eclReference)),
      verifiers = Seq(KeyValue(VerifierKey, dateFormatter.format(subscriptionResponse.success.processingDate)))
    )

  private def executeExtendedAuditEvent(
    businessPartnerId: String,
    subscriptionStatusResponse: SubscriptionStatusResponse,
    internalId: String
  ): Unit =
    auditConnector.sendExtendedEvent(
      SubscriptionStatusRetrievedAuditEvent(
        internalId,
        businessPartnerId,
        AuditSubscriptionStatus(
          subscriptionStatusResponse.subscriptionStatus,
          subscriptionStatusResponse.idValue,
          subscriptionStatusResponse.channel
        )
      ).extendedDataEvent
    )

  def getSubscription(eclRegistrationReference: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] =
    integrationFrameworkConnector.getSubscription(eclRegistrationReference)

}
