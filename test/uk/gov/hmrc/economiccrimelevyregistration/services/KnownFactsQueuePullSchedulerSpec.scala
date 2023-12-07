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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, UpsertKnownFactsRequest}
import uk.gov.hmrc.economiccrimelevyregistration.models.{KeyValue, KnownFactsWorkItem}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.KnownFactsQueueRepository
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import scala.concurrent.Future

class KnownFactsQueuePullSchedulerSpec extends SpecBase {
  val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]
  val mockKnownFactsQueueRepository: KnownFactsQueueRepository       = mock[KnownFactsQueueRepository]

  val scheduler = new KnownFactsQueuePullScheduler(
    ActorSystem.create(),
    mockKnownFactsQueueRepository,
    mockEnrolmentStoreProxyConnector
  )

  "processKnownFacts" should {
    "if there is nothing to process, do nothing and return unit" in {
      when(mockKnownFactsQueueRepository.pullOutstanding(any(), any())).thenReturn(Future.successful(None))

      val result: Unit = scheduler.processKnownFacts

      result shouldBe ()
    }

    "if there is something to process, upsert the known fact and if successful, mark it as completed and delete from the queue" in forAll {
      knownFactsWorkItem: WorkItem[KnownFactsWorkItem] =>
        when(mockKnownFactsQueueRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(knownFactsWorkItem)))

        val expectedUpsertKnownFactsRequest =
          UpsertKnownFactsRequest(verifiers =
            Seq(KeyValue(EclEnrolment.VerifierKey, knownFactsWorkItem.item.eclRegistrationDate))
          )

        when(
          mockEnrolmentStoreProxyConnector.upsertKnownFacts(
            ArgumentMatchers.eq(expectedUpsertKnownFactsRequest),
            ArgumentMatchers.eq(knownFactsWorkItem.item.eclReference)
          )(any())
        ).thenReturn(Future.successful(Right(HttpResponse(OK, "", Map.empty))))

        when(mockKnownFactsQueueRepository.completeAndDelete(ArgumentMatchers.eq(knownFactsWorkItem.id)))
          .thenReturn(Future.successful(true))

        val result: Unit = scheduler.processKnownFacts

        result shouldBe ()

        verify(mockEnrolmentStoreProxyConnector, times(1))
          .upsertKnownFacts(
            ArgumentMatchers.eq(expectedUpsertKnownFactsRequest),
            ArgumentMatchers.eq(knownFactsWorkItem.item.eclReference)
          )(any())

        verify(mockKnownFactsQueueRepository, times(1))
          .completeAndDelete(ArgumentMatchers.eq(knownFactsWorkItem.id))

        reset(mockEnrolmentStoreProxyConnector)
        reset(mockKnownFactsQueueRepository)
    }

    "if there is something to process, upsert the known fact and if unsuccessful, mark it as failed" in forAll {
      knownFactsWorkItem: WorkItem[KnownFactsWorkItem] =>
        when(mockKnownFactsQueueRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(knownFactsWorkItem)))

        val expectedUpsertKnownFactsRequest =
          UpsertKnownFactsRequest(verifiers =
            Seq(KeyValue(EclEnrolment.VerifierKey, knownFactsWorkItem.item.eclRegistrationDate))
          )

        when(
          mockEnrolmentStoreProxyConnector.upsertKnownFacts(
            ArgumentMatchers.eq(expectedUpsertKnownFactsRequest),
            ArgumentMatchers.eq(knownFactsWorkItem.item.eclReference)
          )(any())
        ).thenReturn(
          Future.successful(
            Left(UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
          )
        )

        when(
          mockKnownFactsQueueRepository
            .markAs(ArgumentMatchers.eq(knownFactsWorkItem.id), ArgumentMatchers.eq(ProcessingStatus.Failed), any())
        )
          .thenReturn(Future.successful(true))

        val result: Unit = scheduler.processKnownFacts

        result shouldBe ()

        verify(mockEnrolmentStoreProxyConnector, times(1))
          .upsertKnownFacts(
            ArgumentMatchers.eq(expectedUpsertKnownFactsRequest),
            ArgumentMatchers.eq(knownFactsWorkItem.item.eclReference)
          )(any())

        verify(mockKnownFactsQueueRepository, times(1))
          .markAs(ArgumentMatchers.eq(knownFactsWorkItem.id), ArgumentMatchers.eq(ProcessingStatus.Failed), any())

        reset(mockEnrolmentStoreProxyConnector)
        reset(mockKnownFactsQueueRepository)
    }
  }
}
