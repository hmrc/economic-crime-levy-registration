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

import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.economiccrimelevyregistration.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.Future

class StubTaxEnrolmentsConnector @Inject() extends TaxEnrolmentsConnector {
  override def enrol(createEnrolmentRequest: CreateEnrolmentRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    Future.successful(Left(UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)))

}