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
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait TaxEnrolmentsConnector {
  def enrol(
    createEnrolmentRequest: CreateEnrolmentRequest
  )(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class TaxEnrolmentsConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends BaseConnector
    with TaxEnrolmentsConnector {

  private val taxEnrolmentsUrl: String =
    s"${appConfig.taxEnrolmentsBaseUrl}/tax-enrolments/service/$ServiceName/enrolment"

  override def enrol(
    createEnrolmentRequest: CreateEnrolmentRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    retryFor[Unit]("Tax enrolment")(retryCondition) {
      val authHeader = hc.extraHeaders.find(_._1 == "Authorization")
      httpClient
        .put(url"$taxEnrolmentsUrl")
        .withBody(Json.toJson(createEnrolmentRequest))
        .setHeader("Authorization" -> authHeader.head._2)
        .executeAndContinue
    }
}
