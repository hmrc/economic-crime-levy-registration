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

package uk.gov.hmrc.economiccrimelevyregistration.services.deregister

import org.mockito.ArgumentMatchers.{any, anyString}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.RegistrationError
import uk.gov.hmrc.economiccrimelevyregistration.repositories.deregister.DeregistrationRepository

import scala.concurrent.Future

class DeregistrationServiceSpec extends SpecBase {

  val mockRepository: DeregistrationRepository = mock[DeregistrationRepository]

  val service = new DeregistrationService(mockRepository)

  val testException = new Exception("error")

  "upsertDeregistration" should {
    "return the deregistration if successful" in forAll { deregistration: Deregistration =>
      when(mockRepository.upsert(any())).thenReturn(Future.successful(()))

      val result = await(service.upsertDeregistration(deregistration).value)

      result shouldBe Right(deregistration)
    }

    "return error if failure" in forAll { deregistration: Deregistration =>
      when(mockRepository.upsert(any())).thenReturn(Future.failed(testException))

      val result = await(service.upsertDeregistration(deregistration).value)

      result shouldBe Left(RegistrationError.InternalUnexpectedError(Some(testException)))
    }
  }

  "getDeregistration" should {
    "return the deregistration if found" in forAll { deregistration: Deregistration =>
      when(mockRepository.get(anyString())).thenReturn(Future.successful(Some(deregistration)))

      val result = await(service.getDeregistration(deregistration.internalId).value)

      result shouldBe Right(deregistration)
    }

    "return not found error if deregistration not found" in forAll { deregistration: Deregistration =>
      when(mockRepository.get(anyString())).thenReturn(Future.successful(None))

      val result = await(service.getDeregistration(deregistration.internalId).value)

      result shouldBe Left(RegistrationError.NotFound(deregistration.internalId))
    }

    "return error if failure" in forAll { deregistration: Deregistration =>
      when(mockRepository.get(anyString())).thenReturn(Future.failed(testException))

      val result = await(service.getDeregistration(deregistration.internalId).value)

      result shouldBe Left(RegistrationError.InternalUnexpectedError(Some(testException)))
    }
  }

  "deleteDeregistration" should {
    "return if deregistration successfully deleted" in forAll { deregistration: Deregistration =>
      when(mockRepository.deleteRecord(anyString())).thenReturn(Future.successful(()))

      val result = await(service.deleteDeregistration(deregistration.internalId).value)

      result shouldBe Right()
    }

    "return error if failure" in forAll { deregistration: Deregistration =>
      when(mockRepository.deleteRecord(anyString())).thenReturn(Future.failed(testException))

      val result = await(service.deleteDeregistration(deregistration.internalId).value)

      result shouldBe Left(RegistrationError.InternalUnexpectedError(Some(testException)))
    }
  }
}
