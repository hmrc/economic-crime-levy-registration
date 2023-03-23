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
import uk.gov.hmrc.economiccrimelevyregistration.models.{KeyValue, KnownFactsWorkItem}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionService @Inject() (
  integrationFrameworkConnector: IntegrationFrameworkConnector,
  taxEnrolmentsConnector: TaxEnrolmentsConnector,
  knownFactsQueueRepository: KnownFactsQueueRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault())

  def subscribeToEcl(
    eclSubscription: EclSubscription
  )(implicit hc: HeaderCarrier): Future[CreateEclSubscriptionResponse] =
    integrationFrameworkConnector.subscribeToEcl(eclSubscription).flatMap { response =>
      taxEnrolmentsConnector.enrol(createEnrolmentRequest(response)).flatMap {
        case Left(e)  =>
          logger.error(s"Failed to enrol synchronously: ${e.message}")
          knownFactsQueueRepository
            .pushNew(KnownFactsWorkItem(response.eclReference, dateFormatter.format(response.processingDate)))
            .map { _ =>
              response
            }
        case Right(_) => Future.successful(response)
      }
    }

  private def createEnrolmentRequest(subscriptionResponse: CreateEclSubscriptionResponse): CreateEnrolmentRequest =
    CreateEnrolmentRequest(
      identifiers = Seq(KeyValue(IdentifierKey, subscriptionResponse.eclReference)),
      verifiers = Seq(KeyValue(VerifierKey, dateFormatter.format(subscriptionResponse.processingDate)))
    )
}
