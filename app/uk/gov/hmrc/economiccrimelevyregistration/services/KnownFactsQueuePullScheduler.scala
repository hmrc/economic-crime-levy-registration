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

import akka.actor.ActorSystem
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.KeyValue
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, UpsertKnownFactsRequest}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class KnownFactsQueuePullScheduler @Inject() (
  actorSystem: ActorSystem,
  knownFactsQueueRepository: KnownFactsQueueRepository,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit executionContext: ExecutionContext)
    extends Logging {

  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 10.seconds, interval = 1.minute) { () =>
    processKnownFacts
  }

  def processKnownFacts: Future[Unit] = {
    logger.info("Processing known facts for failed enrolments")

    knownFactsQueueRepository
      .pullOutstanding(
        Instant.now(),
        Instant.now()
      )
      .flatMap {
        case None                     =>
          logger.info("No known facts to process for failed enrolments")
          Future.unit
        case Some(knownFactsWorkItem) =>
          enrolmentStoreProxyConnector
            .upsertKnownFacts(
              UpsertKnownFactsRequest(
                verifiers = Seq(KeyValue(EclEnrolment.VerifierKey, knownFactsWorkItem.item.eclRegistrationDate))
              ),
              knownFactsWorkItem.item.eclReference
            )(HeaderCarrier())
            .map {
              case Left(e)  =>
                logger.error(s"Failed to upsert known facts for failed enrolment: ${e.message}")
                knownFactsQueueRepository.markAs(knownFactsWorkItem.id, ProcessingStatus.Failed)
              case Right(_) =>
                logger.info("Successfully upserted known facts for failed enrolment")
                knownFactsQueueRepository.completeAndDelete(knownFactsWorkItem.id)
            }
      }
  }
}
