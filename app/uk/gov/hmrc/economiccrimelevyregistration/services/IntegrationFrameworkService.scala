/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.economiccrimelevyregistration.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.Successful
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, NotSubscribed, Subscribed}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkService @Inject() (
  integrationFrameworkConnector: IntegrationFrameworkConnector
)(implicit ec: ExecutionContext) {

  def getSubscriptionStatus(
    businessPartnerId: String
  )(implicit hc: HeaderCarrier): Future[EclSubscriptionStatus] =
    integrationFrameworkConnector.getSubscriptionStatus(businessPartnerId).map { subscriptionStatusResponse =>
      (
        subscriptionStatusResponse.subscriptionStatus,
        subscriptionStatusResponse.idType,
        subscriptionStatusResponse.idValue
      ) match {
        case (Successful, Some("ZECL"), Some(eclRegistrationReference))                           =>
          EclSubscriptionStatus(Subscribed(eclRegistrationReference))
        case (Successful, None, None) | (Successful, Some(_), None) | (Successful, None, Some(_)) =>
          throw new IllegalStateException(
            s"Subscription status is Successful but there is no id type or value"
          )
        case (subscriptionStatus, Some(idType), Some(idValue))                                    =>
          throw new IllegalStateException(
            s"Subscription status $subscriptionStatus returned with unexpected idType $idType and value $idValue"
          )
        case _                                                                                    => EclSubscriptionStatus(NotSubscribed)
      }
    }

}
