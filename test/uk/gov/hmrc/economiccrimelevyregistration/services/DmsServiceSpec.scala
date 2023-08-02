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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.http.HeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.ValidNrsSubmission
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{DmsConnector, NrsConnector}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest

import java.time.{Clock, Instant, ZoneId}
import java.util.Base64
import scala.concurrent.{Await, Future}

class DmsServiceSpec extends SpecBase {

  val mockDmsConnector: DmsConnector       = mock[DmsConnector]
  val html = "<html><head></head><body></body></html>"
  val now = Instant.now

  val service = new DmsService(mockDmsConnector)

  "submitToDms" should {
    "return correct value when the submission is successful" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)
      test(Some(encoded), true, true, now)
    }

    "throw an exception if submission fails" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)
      test(Some(encoded), false, false, now, "Could not send PDF to DMS queue")
    }

    "throw an exception if no data to submit" in {
      test(None, false, false, now, "Base64 encoded DMS submission HTML not found in registration data")
    }
  }

  private def test(encoded: Option[String], successful: Boolean, valid: Boolean, instant: Instant, message: String = "") = {
    when(mockDmsConnector.sendPdf(any(), any())(any())).thenReturn(Future.successful(successful))
    try {
      val result = await(service.submitToDms(encoded, now))
      if (valid) {
        result shouldBe CreateEclSubscriptionResponsePayload(now, "")
      } else {
        fail
      }
    } catch {
      case e: Throwable => if (valid) {
        fail
      } else {
        e.getMessage shouldBe message
      }
    }
  }
}
