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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.http.{HttpClient, UpstreamErrorResponse}

import java.time.Duration
import scala.concurrent.Future

class NrsConnectorSpec extends SpecBase {

  val actorSystem: ActorSystem   = ActorSystem("test")
  val mockConfig: Config         = mock[Config]
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new NrsConnectorImpl(appConfig, mockHttpClient, mockConfig, actorSystem)
  val nrsSubmissionUrl: String   = s"${appConfig.nrsBaseUrl}/submission"

  "submitToNrs" should {
    "return a NRS submission response when the http client returns a NRS submission response" in forAll {
      (nrsSubmission: NrsSubmission, nrsSubmissionResponse: NrsSubmissionResponse) =>
        when(
          mockHttpClient.POST[NrsSubmission, NrsSubmissionResponse](
            ArgumentMatchers.eq(nrsSubmissionUrl),
            ArgumentMatchers.eq(nrsSubmission),
            any()
          )(
            any(),
            any(),
            any(),
            any()
          )
        ).thenReturn(Future.successful(nrsSubmissionResponse))

        val result = await(connector.submitToNrs(nrsSubmission))

        result shouldBe nrsSubmissionResponse

        verify(mockHttpClient, times(1))
          .POST[NrsSubmission, NrsSubmissionResponse](
            ArgumentMatchers.eq(nrsSubmissionUrl),
            ArgumentMatchers.eq(nrsSubmission),
            any()
          )(
            any(),
            any(),
            any(),
            any()
          )

        reset(mockHttpClient)
    }

    "return an error if the NRS submission fails after 4 attempts (original attempt + 3 retries)" in forAll {
      nrsSubmission: NrsSubmission =>
        val error = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(mockConfig.getDurationList("http-verbs.retries.intervals")).thenReturn(List[Duration]("1s", "1s", "1s"))

        when(
          mockHttpClient.POST[NrsSubmission, NrsSubmissionResponse](
            ArgumentMatchers.eq(nrsSubmissionUrl),
            ArgumentMatchers.eq(nrsSubmission),
            any()
          )(
            any(),
            any(),
            any(),
            any()
          )
        ).thenReturn(Future.failed(error))

        val result = intercept[UpstreamErrorResponse] {
          await(connector.submitToNrs(nrsSubmission))
        }

        result shouldBe error

        verify(mockHttpClient, times(4))
          .POST[NrsSubmission, NrsSubmissionResponse](
            ArgumentMatchers.eq(nrsSubmissionUrl),
            ArgumentMatchers.eq(nrsSubmission),
            any()
          )(
            any(),
            any(),
            any(),
            any()
          )

        reset(mockHttpClient)
    }
  }

}
