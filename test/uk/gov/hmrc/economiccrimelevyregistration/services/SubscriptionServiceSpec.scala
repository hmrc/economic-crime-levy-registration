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

package uk.gov.hmrc.economiccrimelevyregistration.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IntegrationFrameworkConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription}

import scala.concurrent.Future

class SubscriptionServiceSpec extends SpecBase {
  val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]
  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector               = mock[TaxEnrolmentsConnector]

  val service = new SubscriptionService(mockIntegrationFrameworkConnector, mockTaxEnrolmentsConnector)

  "subscribeToEcl" should {
    "return the ECL reference number" in forAll {
      (
        eclSubscription: EclSubscription,
        subscriptionResponse: CreateEclSubscriptionResponse
      ) =>
        when(mockIntegrationFrameworkConnector.subscribeToEcl(ArgumentMatchers.eq(eclSubscription))(any()))
          .thenReturn(Future.successful(subscriptionResponse))

        when(mockTaxEnrolmentsConnector.enrol(any())(any()))
          .thenReturn(Future.successful(()))

        val result = await(service.subscribeToEcl(eclSubscription))

        result shouldBe subscriptionResponse
    }
  }
}