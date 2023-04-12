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

import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, EnrolmentGroupIdResponse}
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyEnrolmentStoreProxyConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient)(implicit
                                                                                                   ec: ExecutionContext
) {

  private val enrolmentStoreUrl: String =
    s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store/"

  def fetchGroupId(eclReference: String)(implicit hc: HeaderCarrier): Future[EnrolmentGroupIdResponse] =
    httpClient
      .GET[EnrolmentGroupIdResponse](
        s"$enrolmentStoreUrl/enrolments/${EclEnrolment.EnrolmentKey(eclReference)}/groups"
      )

  def deEnrol(groupId: String, eclReference: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .DELETE[HttpResponse](
        s"$enrolmentStoreUrl/groups/$groupId/enrolments/${EclEnrolment.EnrolmentKey(eclReference)}"
      ).map(_ => ())
}
