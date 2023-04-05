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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.NrsConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class NrsServiceSpec extends SpecBase {

  val mockNrsConnector: NrsConnector = mock[NrsConnector]
  private val fixedPointInTime       = Instant.parse("2007-12-25T10:15:30.00Z")
  private val stubClock: Clock       = Clock.fixed(fixedPointInTime, ZoneId.systemDefault)

  val service = new NrsService(mockNrsConnector, stubClock)

  "submitToNrs" should {
    "return the NRS submission ID when the submission is successful" in forAll {
      (
        nrsSubmission: NrsSubmission,
        nrsSubmissionResponse: NrsSubmissionResponse,
        base64EncodedNrsSubmissionHtml: String,
        eclRegistrationReference: String,
        businessPartnerId: String,
        internalId: String
      ) =>
        when(mockNrsConnector.submitToNrs(ArgumentMatchers.eq(nrsSubmission))(any()))
          .thenReturn(Future.successful(nrsSubmissionResponse))

        val request = AuthorisedRequest(fakeRequest, internalId, nrsSubmission.metadata.identityData)

        val result =
          await(
            service.submitToNrs(Some(base64EncodedNrsSubmissionHtml), eclRegistrationReference, businessPartnerId)(
              hc,
              request
            )
          )

        result shouldBe nrsSubmissionResponse
    }

    "throw an IllegalStateException when there is no base64 encoded NRS submission HTML" in forAll {
      (
        eclRegistrationReference: String,
        businessPartnerId: String,
        internalId: String,
        nrsIdentityData: NrsIdentityData
      ) =>
        val request = AuthorisedRequest(fakeRequest, internalId, nrsIdentityData)

        val result = intercept[IllegalStateException] {
          await(service.submitToNrs(None, eclRegistrationReference, businessPartnerId)(hc, request))
        }

        result.getMessage shouldBe "Base64 encoded NRS submission HTML not found in registration data"
    }

    "throw an exception when the submission fails" in forAll {
      (
        nrsSubmission: NrsSubmission,
        base64EncodedNrsSubmissionHtml: String,
        eclRegistrationReference: String,
        businessPartnerId: String,
        internalId: String,
        nrsIdentityData: NrsIdentityData
      ) =>
        val error = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(mockNrsConnector.submitToNrs(ArgumentMatchers.eq(nrsSubmission))(any()))
          .thenReturn(Future.failed(error))

        val request = AuthorisedRequest(fakeRequest, internalId, nrsIdentityData)

        val result = intercept[UpstreamErrorResponse] {
          await(
            service.submitToNrs(Some(base64EncodedNrsSubmissionHtml), eclRegistrationReference, businessPartnerId)(
              hc,
              request
            )
          )
        }

        result shouldBe error
    }
  }

}
