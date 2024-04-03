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
import uk.gov.hmrc.economiccrimelevyregistration.ValidNrsSubmission
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.NrsConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.NrsSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.{HeaderNames, UpstreamErrorResponse}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class NrsServiceSpec extends SpecBase {

  val mockNrsConnector: NrsConnector       = mock[NrsConnector]
  private val fixedPointInTime             = Instant.parse("2007-12-25T10:15:30.00Z")
  private val stubClock: Clock             = Clock.fixed(fixedPointInTime, ZoneId.systemDefault)
  private val fakeRequestWithAuthorisation = fakeRequest.withHeaders((HeaderNames.authorisation, "test"))
  val mockAppConfig: AppConfig             = mock[AppConfig]
  val service                              = new NrsService(mockNrsConnector, stubClock)

  "submitToNrs" should {
    "return the NRS submission ID when the submission is successful" in forAll(
      arbValidNrsSubmission(fakeRequestWithAuthorisation, stubClock).arbitrary,
      Arbitrary.arbitrary[NrsSubmissionResponse]
    ) { (validNrsSubmission: ValidNrsSubmission, nrsSubmissionResponse: NrsSubmissionResponse) =>
      when(
        mockNrsConnector.submitToNrs(
          ArgumentMatchers.eq(
            validNrsSubmission.nrsSubmission.copy(metadata =
              validNrsSubmission.nrsSubmission.metadata
                .copy(notableEvent = mockAppConfig.eclFirstTimeRegistrationNotableEvent)
            )
          )
        )(any())
      )
        .thenReturn(Future.successful(nrsSubmissionResponse))

      val request = AuthorisedRequest(
        fakeRequestWithAuthorisation,
        validNrsSubmission.nrsSubmission.metadata.identityData.internalId,
        validNrsSubmission.nrsSubmission.metadata.identityData
      )

      val result =
        await(
          service
            .submitToNrs(
              Some(validNrsSubmission.base64EncodedNrsSubmissionHtml),
              validNrsSubmission.eclRegistrationReference,
              mockAppConfig.eclFirstTimeRegistrationNotableEvent
            )(hc, request)
            .value
        )

      result shouldBe Right(nrsSubmissionResponse)
    }

    "throw an IllegalStateException when there is no base64 encoded NRS submission HTML" in forAll(
      arbValidNrsSubmission(fakeRequestWithAuthorisation, stubClock).arbitrary
    ) { validNrsSubmission: ValidNrsSubmission =>
      val request = AuthorisedRequest(
        fakeRequestWithAuthorisation,
        validNrsSubmission.nrsSubmission.metadata.identityData.internalId,
        validNrsSubmission.nrsSubmission.metadata.identityData
      )

      val result = await(
        service
          .submitToNrs(
            None,
            validNrsSubmission.eclRegistrationReference,
            mockAppConfig.eclFirstTimeRegistrationNotableEvent
          )(hc, request)
          .value
      )

      result shouldBe Left(
        NrsSubmissionError
          .InternalUnexpectedError(None)
      )
    }
  }

  "submit" should {
    "return Left - NrsSubmissionError.BadGateway when call to nrsConnector fails with 4xx error" in forAll {
      (nrsSubmission: NrsSubmission) =>
        val errorMessage = "Error message"
        when(mockNrsConnector.submitToNrs(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, BAD_REQUEST)))

        val result = await(service.submit(nrsSubmission)(hc).value)

        result shouldBe Left(NrsSubmissionError.BadGateway(errorMessage, BAD_REQUEST))
    }

    "return Left - NrsSubmissionError.BadGateway when call to nrsConnector fails with 5xx error" in forAll {
      (nrsSubmission: NrsSubmission) =>
        val errorMessage = "Error message"
        when(mockNrsConnector.submitToNrs(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, BAD_GATEWAY)))

        val result = await(service.submit(nrsSubmission)(hc).value)

        result shouldBe Left(NrsSubmissionError.BadGateway(errorMessage, BAD_GATEWAY))
    }

    "return Left - NrsSubmissionError.InternalUnexpectedError when call to nrsConnector fails with NonFatal error" in forAll {
      (nrsSubmission: NrsSubmission) =>
        val errorMessage = "Error message"
        val exception    = new Exception(errorMessage)
        when(mockNrsConnector.submitToNrs(any())(any()))
          .thenReturn(Future.failed(exception))

        val result = await(service.submit(nrsSubmission)(hc).value)

        result shouldBe Left(NrsSubmissionError.InternalUnexpectedError(Some(exception)))
    }
  }

}
