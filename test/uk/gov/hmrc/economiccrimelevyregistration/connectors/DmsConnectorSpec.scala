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

import akka.stream.scaladsl.Source
import org.mockito.ArgumentMatchers.any
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class DmsConnectorSpec extends SpecBase {

  override def configOverrides(): Map[String, Any] = Map(
    "http-verbs.retries.intervals" -> List("1ms")
  )

  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val connector                          = new DmsConnector(
    mockHttpClient,
    appConfig
  )

  "sendPdf" should {
    "return true if post to DMS queue succeeds" in {
      test(ACCEPTED, true)
    }

    "return false if post to DMS queue fails" in {
      test(INTERNAL_SERVER_ERROR, false)
    }
  }

  private def test(status: Int, expected: Boolean) = {
    implicit val request = FakeRequest("", "")

    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(TestResponse(status)))

    val result = await(connector.sendPdf(Source(Seq.empty)))

    result shouldBe expected

    verify(mockRequestBuilder, times(1))
      .execute(any(), any())

    reset(mockRequestBuilder)
  }
}

case class TestResponse(
  status: Int
) extends HttpResponse {
  override def body: String                         = ""
  override def allHeaders: Map[String, Seq[String]] = Map.empty
}
