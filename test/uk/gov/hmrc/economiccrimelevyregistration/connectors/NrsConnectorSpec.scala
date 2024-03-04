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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import org.apache.pekko.actor.ActorSystem
import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class NrsConnectorSpec extends SpecBase {

  val actorSystem: ActorSystem           = ActorSystem("test")
  val config: Config                     = app.injector.instanceOf[Config]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val connector                          = new NrsConnector(appConfig, mockHttpClient, config, actorSystem)
  val nrsSubmissionUrl: String           = s"${appConfig.nrsBaseUrl}/submission"

  "submitToNrs" should {
    "return a NRS submission response when the http client returns a NRS submission response" in forAll {
      (nrsSubmission: NrsSubmission, nrsSubmissionResponse: NrsSubmissionResponse) =>
        val expectedResponse = HttpResponse.apply(ACCEPTED, Json.stringify(Json.toJson(nrsSubmissionResponse)))

        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(expectedResponse))

        val result = await(connector.submitToNrs(nrsSubmission))

        result shouldBe nrsSubmissionResponse

        verify(mockRequestBuilder, times(1))
          .execute(any(), any())

        reset(mockHttpClient)
        reset(mockRequestBuilder)
    }
  }

}
