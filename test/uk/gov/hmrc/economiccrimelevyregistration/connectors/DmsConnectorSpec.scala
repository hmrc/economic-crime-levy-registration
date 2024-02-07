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
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class DmsConnectorSpec extends SpecBase {

  override def configOverrides: Map[String, Any] = Map(
    "http-verbs.retries.intervals" -> List("1ms")
  )

  val actorSystem: ActorSystem           = ActorSystem("test")
  val config: Config                     = app.injector.instanceOf[Config]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val connector                          = new DmsConnector(
    mockHttpClient,
    appConfig,
    config,
    actorSystem
  )

  "sendPdf" should {
    "return HttpResponse if post to DMS queue succeeds" in {
      val expectedResponse = HttpResponse.apply(ACCEPTED, "")

      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result: Unit = await(connector.sendPdf(Source(Seq.empty)))

      result shouldBe ()

      verify(mockRequestBuilder, times(1))
        .execute(any(), any())

      reset(mockRequestBuilder)
    }

    "return UpstreamErrorResponse if post to DMS queue fails" in {

      val expectedResponse = HttpResponse.apply(BAD_REQUEST, "")

      val upstream5xxResponse = UpstreamErrorResponse.apply("", BAD_REQUEST)

      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = intercept[UpstreamErrorResponse](await(connector.sendPdf(Source(Seq.empty))))

      result shouldBe upstream5xxResponse

      verify(mockRequestBuilder, times(1))
        .execute(any(), any())

      reset(mockRequestBuilder)
    }
  }
}
