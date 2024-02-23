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

package uk.gov.hmrc.economiccrimelevyregistration.repositories.deregister

import org.mockito.MockitoSugar
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class DeregistrationRepositorySpec
    extends AnyWordSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[Deregistration]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val now              = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault)

  private val deregistration = Deregistration
    .empty(
      internalId = "test-id"
    )
    .copy(lastUpdated = Some(Instant.ofEpochSecond(1)))

  private val mockAppConfig = mock[AppConfig]

  when(mockAppConfig.mongoTtl) thenReturn 1

  protected override val repository = new DeregistrationRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  "upsert" should {
    "insert a new deregistration with the last updated time set to `now`" in {
      val expectedResult = deregistration.copy(lastUpdated = Some(now))

      val setResult     = repository.upsert(deregistration).futureValue
      val updatedRecord = find(Filters.equal("internalId", deregistration.internalId)).futureValue.headOption.value

      setResult     shouldEqual ()
      updatedRecord shouldEqual expectedResult
    }

    "update an existing deregistration with the last updated time set to `now`" in {
      insert(deregistration).futureValue

      val expectedResult = deregistration.copy(lastUpdated = Some(now))

      val setResult     = repository.upsert(deregistration).futureValue
      val updatedRecord = find(Filters.equal("internalId", deregistration.internalId)).futureValue.headOption.value

      setResult     shouldEqual ()
      updatedRecord shouldEqual expectedResult
    }
  }

  "get" should {
    "update the lastUpdated time and get the record when there is a record for the id" in {
      insert(deregistration.copy(lastUpdated = Some(now))).futureValue

      val result         = repository.get(deregistration.internalId).futureValue
      val expectedResult = deregistration.copy(lastUpdated = Some(now))

      result.value shouldEqual expectedResult
    }

    "return None when there is no record for the id" in {
      repository.get("id that does not exist").futureValue should not be defined
    }
  }

  "deleteRecord" should {
    "remove a record" in {
      insert(deregistration).futureValue

      val result = repository.deleteRecord(deregistration.internalId).futureValue

      result                                           shouldEqual ()
      repository.get(deregistration.internalId).futureValue should not be defined
    }

    "return true when there is no record to remove" in {
      val result = repository.deleteRecord("id that does not exist").futureValue

      result shouldEqual ()
    }
  }

  "keepAlive" should {
    "update lastUpdated to `now` and return true when there is a record for the id" in {
      insert(deregistration).futureValue

      val result = repository.keepAlive(deregistration.internalId).futureValue

      val expectedRegistration = deregistration.copy(lastUpdated = Some(now))

      result shouldEqual true
      val updatedRegistration =
        find(Filters.equal("internalId", deregistration.internalId)).futureValue.headOption.value
      updatedRegistration shouldEqual expectedRegistration
    }

    "return true when there is no record for the id" in {
      repository.keepAlive("id that does not exist").futureValue shouldEqual true
    }
  }
}
