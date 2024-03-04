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

import org.apache.pekko.actor.ActorSystem
import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.{KeyValue, KnownFactsWorkItem}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, UpsertKnownFactsRequest}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{KnownFactsError, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class KnownFactsQueuePullScheduler @Inject() (
  actorSystem: ActorSystem,
  knownFactsQueueRepository: KnownFactsQueueRepository,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit executionContext: ExecutionContext)
    extends Logging
    with ErrorHandler {

  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 10.seconds, interval = 1.minute) { () =>
    processKnownFacts
  }

  def processKnownFacts: EitherT[Future, ResponseError, Unit] = {
    logger.info("Processing known facts for failed enrolments")
    for {
      workItem <- pullOutstandingWorkItem.asResponseError
      _        <- upsertKnownFacts(workItem).asResponseError
    } yield ()
  }
  private def pullOutstandingWorkItem: EitherT[Future, KnownFactsError, WorkItem[KnownFactsWorkItem]] = {
    val now: Instant = Instant.now()
    EitherT {
      knownFactsQueueRepository
        .pullOutstanding(now, now)
        .map {
          case None        =>
            Left(KnownFactsError.NotFound("No known facts to process for failed enrolments"))
          case Some(value) => Right(value)
        }
    }
  }

  private def upsertKnownFacts(
    workItem: WorkItem[KnownFactsWorkItem]
  ): EitherT[Future, KnownFactsError, Unit] =
    EitherT {
      val upsertKnownFactsRequest = UpsertKnownFactsRequest(
        verifiers = Seq(KeyValue(EclEnrolment.VerifierKey, workItem.item.eclRegistrationDate))
      )

      enrolmentStoreProxyConnector
        .upsertKnownFacts(
          upsertKnownFactsRequest,
          workItem.item.eclReference
        )(HeaderCarrier())
        .map {
          logger.info("Successfully upserted known facts for failed enrolment")
          knownFactsQueueRepository.completeAndDelete(workItem.id)
          Right(_)
        }
        .recover { case error =>
          knownFactsQueueRepository.markAs(workItem.id, ProcessingStatus.Failed)
          Left(
            KnownFactsError
              .UpsertKnownFactsError(s"Failed to upsert known facts for failed enrolment: ${error.getMessage}")
          )
        }
    }
}
