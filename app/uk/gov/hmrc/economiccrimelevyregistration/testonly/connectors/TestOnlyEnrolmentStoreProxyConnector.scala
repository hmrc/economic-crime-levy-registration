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

import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.testonly.models.EnrolmentGroupIdResponse
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyEnrolmentStoreProxyConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val enrolmentStoreUrl: String =
    s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  private def readOptionOfNotFoundOrNoContent[A: HttpReads]: HttpReads[Option[A]] = HttpReads[HttpResponse]
    .flatMap(_.status match {
      case NOT_FOUND | NO_CONTENT => HttpReads.pure(None)
      case _                      =>
        HttpReads[A].map(Some.apply)
    })

  def getAllocatedPrincipalGroupIds(
    eclReference: String
  )(implicit hc: HeaderCarrier): Future[Option[EnrolmentGroupIdResponse]] = {
    val url =
      s"$enrolmentStoreUrl/enrolments/${EclEnrolment.enrolmentKey(eclReference)}/groups?type=principal&ignore-assignments=true"
    httpClient
      .get(url"$url")
      .execute[Option[EnrolmentGroupIdResponse]](readOptionOfNotFoundOrNoContent, ec)
  }

  def deEnrol(groupId: String, eclReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val url = s"$enrolmentStoreUrl/groups/$groupId/enrolments/${EclEnrolment.enrolmentKey(eclReference)}"
    httpClient
      .delete(url"$url")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
  }
}
