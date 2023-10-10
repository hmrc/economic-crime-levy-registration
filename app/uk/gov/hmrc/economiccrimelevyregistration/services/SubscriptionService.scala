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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IntegrationFrameworkConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription}
import uk.gov.hmrc.economiccrimelevyregistration.models.{KeyValue, KnownFactsWorkItem, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionService @Inject() (
  integrationFrameworkConnector: IntegrationFrameworkConnector,
  taxEnrolmentsConnector: TaxEnrolmentsConnector,
  knownFactsQueueRepository: KnownFactsQueueRepository,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

  def subscribeToEcl(
    eclSubscription: EclSubscription,
    registration: Registration,
    liabilityYear: Option[Int]
  )(implicit hc: HeaderCarrier): Future[CreateEclSubscriptionResponse] =
    integrationFrameworkConnector.subscribeToEcl(eclSubscription).flatMap {
      case Right(createSubscriptionSuccessResponse) =>
        taxEnrolmentsConnector.enrol(createEnrolmentRequest(createSubscriptionSuccessResponse)).flatMap {
          case Left(e)  =>
            logger.error(s"Failed to enrol synchronously: ${e.message}")
            auditService.successfulSubscriptionFailedEnrolment(
              registration,
              createSubscriptionSuccessResponse.success.eclReference,
              e.getMessage(),
              liabilityYear
            )

            knownFactsQueueRepository
              .pushNew(
                KnownFactsWorkItem(
                  createSubscriptionSuccessResponse.success.eclReference,
                  dateFormatter.format(createSubscriptionSuccessResponse.success.processingDate)
                )
              )
              .map { _ =>
                createSubscriptionSuccessResponse
              }
          case Right(_) =>
            auditService
              .successfulSubscriptionAndEnrolment(
                registration,
                createSubscriptionSuccessResponse.success.eclReference,
                liabilityYear
              )
            Future.successful(createSubscriptionSuccessResponse)
        }
      case Left(e)                                  =>
        auditService.failedSubscription(registration, e.getMessage(), liabilityYear)
        throw e
    }

  private def createEnrolmentRequest(subscriptionResponse: CreateEclSubscriptionResponse): CreateEnrolmentRequest =
    CreateEnrolmentRequest(
      identifiers = Seq(KeyValue(IdentifierKey, subscriptionResponse.success.eclReference)),
      verifiers = Seq(KeyValue(VerifierKey, dateFormatter.format(subscriptionResponse.success.processingDate)))
    )
}
