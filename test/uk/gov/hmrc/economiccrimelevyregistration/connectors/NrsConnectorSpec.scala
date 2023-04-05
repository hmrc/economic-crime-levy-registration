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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.http.HttpClient

class NrsConnectorSpec extends SpecBase {

  val actorSystem: ActorSystem   = ActorSystem("test")
  val mockConfig: Config         = mock[Config]
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new NrsConnectorImpl(appConfig, mockHttpClient, mockConfig, actorSystem)
  val nrsSubmissionUrl: String   = s"${appConfig.nrsBaseUrl}/submission"

  "submitToNrs" should {
    "" in {
      pending
    }
  }

}
