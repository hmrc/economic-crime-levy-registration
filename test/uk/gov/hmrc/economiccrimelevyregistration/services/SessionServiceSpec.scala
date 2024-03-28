/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionData
import uk.gov.hmrc.economiccrimelevyregistration.repositories.SessionRepository
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError

import java.util.UUID
import scala.concurrent.Future

class SessionServiceSpec extends SpecBase {

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  val service = new SessionService(mockSessionRepository)

  "get" should {
    "return Right with correct value when call to repository is successful" in forAll { (sessionData: SessionData) =>
      val id = UUID.randomUUID().toString

      when(mockSessionRepository.get(ArgumentMatchers.eq(id)))
        .thenReturn(Future.successful(Some(sessionData)))

      val result = await(service.get(id).value)

      result shouldBe Right(sessionData)
    }

    "return Left with DataRetrievalError.NotFound when call to repository returns None" in {
      val id = UUID.randomUUID().toString

      when(mockSessionRepository.get(ArgumentMatchers.eq(id)))
        .thenReturn(Future.successful(None))

      val result = await(service.get(id).value)

      result shouldBe Left(DataRetrievalError.NotFound(id))
    }
  }

  "upsert" should {
    "return Right with correct value when call to repository is successful" in forAll { (sessionData: SessionData) =>
      when(mockSessionRepository.upsert(ArgumentMatchers.eq(sessionData)))
        .thenReturn(Future.successful(()))

      val result = await(service.upsert(sessionData).value)

      result shouldBe Right(())
    }

    "return Left with DataRetrievalError.InternalUnexpectedError when call to repository fails" in forAll {
      (sessionData: SessionData) =>
        val message   = "Error message"
        val exception = new Exception(message)

        when(mockSessionRepository.upsert(ArgumentMatchers.eq(sessionData)))
          .thenReturn(Future.failed(exception))

        val result = await(service.upsert(sessionData).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(message, Some(exception)))
    }
  }

  "delete" should {
    "return Right with correct value when call to repository is successful" in {

      val id = UUID.randomUUID().toString

      when(mockSessionRepository.deleteRecord(ArgumentMatchers.eq(id)))
        .thenReturn(Future.successful(()))

      val result = await(service.delete(id).value)

      result shouldBe Right(())
    }

    "return Left with DataRetrievalError.InternalUnexpectedError when call to repository fails" in {

      val message   = "Error message"
      val exception = new Exception(message)
      val id        = UUID.randomUUID().toString

      when(mockSessionRepository.deleteRecord(ArgumentMatchers.eq(id)))
        .thenReturn(Future.failed(exception))

      val result = await(service.delete(id).value)

      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(message, Some(exception)))
    }
  }
}
