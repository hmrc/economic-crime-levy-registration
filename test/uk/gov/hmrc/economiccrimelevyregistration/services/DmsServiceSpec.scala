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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.DmsConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.Instant
import java.util.Base64
import scala.concurrent.Future

class DmsServiceSpec extends SpecBase {

  val mockDmsConnector: DmsConnector = mock[DmsConnector]
  val html                           = "<html><head></head><body></body></html>"
  val now                            = Instant.now

  val service = new DmsService(mockDmsConnector, appConfig)

  "submitToDms" should {
    "return correct value when the submission is successful" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.successful(Right(())))

      val result = await(service.submitToDms(Some(encoded), now))

      result shouldBe Right(CreateEclSubscriptionResponsePayload(now, ""))
    }

    "return upstream error if submission fails" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val upstream5xxResponse = UpstreamErrorResponse.apply("", INTERNAL_SERVER_ERROR)
      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.successful(Left(upstream5xxResponse)))

      val result = await(service.submitToDms(Some(encoded), now))

      result shouldBe Left(upstream5xxResponse)
    }

    "return upstream error if no data to submit" in {

      val upstream5xxResponse = UpstreamErrorResponse.apply(
        "Base64 encoded DMS submission HTML not found in registration data",
        INTERNAL_SERVER_ERROR
      )

      val result = await(service.submitToDms(None, now))

      result shouldBe Left(upstream5xxResponse)
    }
  }
}
