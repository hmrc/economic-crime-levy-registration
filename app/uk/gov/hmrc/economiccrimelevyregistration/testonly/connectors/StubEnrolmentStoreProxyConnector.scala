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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.connectors

import play.api.http.Status
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.UpsertKnownFactsRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.Future

class StubEnrolmentStoreProxyConnector @Inject() extends EnrolmentStoreProxyConnector {
  override def upsertKnownFacts(upsertKnownFactsRequest: UpsertKnownFactsRequest, eclReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = Future.successful((HttpResponse(Status.NO_CONTENT, "", Map.empty)))
}
