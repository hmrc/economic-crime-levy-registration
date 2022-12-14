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
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest.serviceName

import scala.concurrent.Future

class TaxEnrolmentsConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new TaxEnrolmentsConnector(appConfig, mockHttpClient)

  "enrol" should {
    "return unit when the http client successfully returns a http response" in forAll {
      taxEnrolment: CreateEnrolmentRequest =>
        val expectedUrl = s"${appConfig.taxEnrolmentsBaseUrl}/tax-enrolments/service/$serviceName/enrolment"

        val response = HttpResponse(NO_CONTENT, "", Map.empty)

        when(
          mockHttpClient
            .PUT[CreateEnrolmentRequest, HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any(),
              any()
            )
        )
          .thenReturn(Future.successful(response))

        val result: Unit = await(connector.enrol(taxEnrolment))

        result shouldBe ()

        verify(mockHttpClient, times(1))
          .PUT[CreateEnrolmentRequest, HttpResponse](ArgumentMatchers.eq(expectedUrl), any(), any())(
            any(),
            any(),
            any(),
            any()
          )

        reset(mockHttpClient)
    }
  }
}
