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

import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.CustomHeaderNames
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs.{NrsSubmission, NrsSubmissionResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait NrsConnector {
  def submitToNrs(nrsSubmission: NrsSubmission)(implicit
    hc: HeaderCarrier
  ): Future[NrsSubmissionResponse]
}

@Singleton
class NrsConnectorImpl @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends NrsConnector {

  private val nrsSubmissionUrl: String = s"${appConfig.nrsBaseUrl}/submission"

  private def nrsHeaders: Seq[(String, String)] = Seq(
    (HeaderNames.CONTENT_TYPE, MimeTypes.JSON),
    (CustomHeaderNames.ApiKey, appConfig.nrsApiKey)
  )

  override def submitToNrs(nrsSubmission: NrsSubmission)(implicit
    hc: HeaderCarrier
  ): Future[NrsSubmissionResponse] =
    httpClient.POST[NrsSubmission, NrsSubmissionResponse](nrsSubmissionUrl, nrsSubmission, headers = nrsHeaders)
}
