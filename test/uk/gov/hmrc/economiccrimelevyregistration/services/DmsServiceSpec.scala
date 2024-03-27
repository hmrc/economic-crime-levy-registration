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
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DmsSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import java.time.Instant
import java.util.Base64
import scala.concurrent.Future

class DmsServiceSpec extends SpecBase {

  val mockDmsConnector: DmsConnector = mock[DmsConnector]
  val html                           = "<html><head></head><body></body></html>"
  val now: Instant                   = Instant.now

  val service = new DmsService(mockDmsConnector, appConfig)

  "submitToDms" should {
    "return correct value when the submission is successful" in {
      val encoded          = Base64.getEncoder.encodeToString(html.getBytes)
      val expectedResponse = HttpResponse.apply(ACCEPTED, "")

      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.successful(Right(expectedResponse)))

      val result = await(service.submitToDms(Some(encoded), now, RegistrationType.Initial).value)

      result shouldBe Right(CreateEclSubscriptionResponsePayload(now, ""))
    }

    "return upstream error if submission fails" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val upstream5xxResponse = UpstreamErrorResponse.apply("Error message", BAD_GATEWAY)
      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.failed(upstream5xxResponse))

      val result = await(service.submitToDms(Some(encoded), now, RegistrationType.Initial).value)

      result shouldBe Left(DmsSubmissionError.BadGateway("Error message", BAD_GATEWAY))
    }

    "return upstream error if submission fails with 4xx code" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val upstream4xxResponse = UpstreamErrorResponse.apply("Error message", BAD_REQUEST)
      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.failed(upstream4xxResponse))

      val result = await(service.submitToDms(Some(encoded), now, RegistrationType.Initial).value)

      result shouldBe Left(DmsSubmissionError.BadGateway("Error message", BAD_REQUEST))
    }

    "return upstream error if submission fails with NonFatal code" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val nonFatalErrorResponse = new Exception("Error message")

      when(mockDmsConnector.sendPdf(any())(any())).thenReturn(Future.failed(nonFatalErrorResponse))

      val result = await(service.submitToDms(Some(encoded), now, RegistrationType.Initial).value)

      result shouldBe Left(DmsSubmissionError.InternalUnexpectedError(Some(nonFatalErrorResponse)))
    }

    "return upstream error if no data to submit" in {

      val result = await(service.submitToDms(None, now, RegistrationType.Initial).value)

      result shouldBe Left(
        DmsSubmissionError.BadGateway("base64EncodedDmsSubmissionHtml field not provided", BAD_GATEWAY)
      )
    }
  }
}
