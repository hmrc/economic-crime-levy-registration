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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, UpsertKnownFactsRequest}
import uk.gov.hmrc.http.{HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._

import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EnrolmentStoreProxyConnectorImpl(appConfig, mockHttpClient)
  val enrolmentStoreUrl: String  = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  "upsertKnownFacts" should {

    "return either an upstream error response or http response" in forAll {
      (
        eclReference: String,
        upsertKnownFactsRequest: UpsertKnownFactsRequest,
        eitherResult: Either[UpstreamErrorResponse, HttpResponse]
      ) =>
        val expectedUrl = s"$enrolmentStoreUrl/enrolments/${EclEnrolment.EnrolmentKey(eclReference)}"

        when(
          mockHttpClient
            .PUT[UpsertKnownFactsRequest, Either[UpstreamErrorResponse, HttpResponse]](
              ArgumentMatchers.eq(expectedUrl),
              ArgumentMatchers.eq(upsertKnownFactsRequest),
              any()
            )(
              any(),
              any(),
              any(),
              any()
            )
        )
          .thenReturn(Future.successful(eitherResult))

        val result = await(connector.upsertKnownFacts(upsertKnownFactsRequest, eclReference))

        result shouldBe eitherResult

        verify(mockHttpClient, times(1))
          .PUT[UpsertKnownFactsRequest, Either[UpstreamErrorResponse, HttpResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(upsertKnownFactsRequest),
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
