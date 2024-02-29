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

package uk.gov.hmrc.economiccrimelevyregistration.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = configuration.get[String]("appName")

  val eclFirstTimeRegistrationNotableEvent: String =
    configuration.get[String]("microservice.services.nrs.notable-events.ecl-first-time-registration")

  val eclAmendRegistrationNotableEvent: String =
    configuration.get[String]("microservice.services.nrs.notable-events.ecl-amend-registration")

  val appBaseUrl: String = servicesConfig.baseUrl("self")

  val mongoTtl: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val taxEnrolmentsBaseUrl: String = servicesConfig.baseUrl("tax-enrolments")

  val integrationFrameworkUrl: String = servicesConfig.baseUrl("integration-framework")

  val integrationFrameworkBearerToken: String =
    configuration.get[String]("microservice.services.integration-framework.bearerToken")

  val getSubscriptionStatusBearerToken: String =
    configuration.get[String]("microservice.services.integration-framework.getSubscriptionStatusBearerToken")

  val integrationFrameworkEnvironment: String =
    configuration.get[String]("microservice.services.integration-framework.environment")

  val integrationFrameworkGetSubscriptionBearerToken: String =
    configuration.get[String]("microservice.services.integration-framework.getSubscriptionBearerToken")

  val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")

  val eclStubsBaseUrl: String = servicesConfig.baseUrl("economic-crime-levy-stubs")

  val knownFactsInProgressRetryAfter: FiniteDuration =
    configuration.get[FiniteDuration]("knownFactsQueue.inProgressRetryAfter")

  val nrsBaseUrl: String = servicesConfig.baseUrl("nrs")

  val nrsApiKey: String = configuration.get[String]("microservice.services.nrs.apiKey")

  val internalAuthToken: String = configuration.get[String]("internal-auth.token")

  val internalAuthBaseUrl: String = servicesConfig.baseUrl("internal-auth")

  val retryDuration: Iterable[Duration] =
    configuration.underlying.getStringList("http-verbs.retries.intervals").asScala.map(Duration(_))

  val dmsBaseUrl: String                      = servicesConfig.baseUrl("dms-submission")
  val dmsSubmissionBusinessArea: String       =
    configuration.get[String]("microservice.services.dms-submission.registration-submission.businessArea")
  val dmsSubmissionCallbackEndpoint: String   =
    configuration.get[String](
      "microservice.services.dms-submission.registration-submission.callbackEndpoint"
    )
  val dmsSubmissionCallbackUrl: String        = s"$appBaseUrl/$appName/$dmsSubmissionCallbackEndpoint"
  val dmsSubmissionClassificationType: String =
    configuration.get[String]("microservice.services.dms-submission.registration-submission.classificationType")
  val dmsSubmissionCustomerId: String         =
    configuration.get[String]("microservice.services.dms-submission.registration-submission.customerId")
  val dmsSubmissionFormId: String             =
    configuration.get[String]("microservice.services.dms-submission.registration-submission.formId")
  val dmsSubmissionSource: String             =
    configuration.get[String]("microservice.services.dms-submission.registration-submission.source")
  val dmsSubmissionUrl: String                = dmsBaseUrl + "/dms-submission/submit"

  val dmsSubmissionDeregistrationFormId: String =
    configuration.get[String]("microservice.services.dms-submission.registration-submission.deregistrationFormId")

  val amendRegistrationNrsEnabled: Boolean = configuration.get[Boolean]("features.amendRegistrationNrsEnabled")

  val nrsSubmissionEnabled: Boolean = configuration.get[Boolean]("features.nrsSubmissionEnabled")
}
