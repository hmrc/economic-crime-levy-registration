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

package uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._

class SubscriptionStatusResponseSpec extends SpecBase {

  "toEclSubscriptionStatus" should {
    "return Subscribed with the ECL registration reference when the id type is ZECL" in forAll {
      (idValue: String, channel: Option[Channel], etmpSubscriptionStatus: EtmpSubscriptionStatus) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = etmpSubscriptionStatus,
          idType = Some("ZECL"),
          idValue = Some(idValue),
          channel = channel
        )

        val result = subscriptionStatusResponse.toEclSubscriptionStatus

        result shouldBe EclSubscriptionStatus(Subscribed(idValue))
    }

    "return NotSubscribed when there is no id type or value" in forAll {
      (
        subscriptionStatus: EtmpSubscriptionStatus,
        channel: Option[Channel]
      ) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = subscriptionStatus,
          idType = None,
          idValue = None,
          channel = channel
        )

        val result = subscriptionStatusResponse.toEclSubscriptionStatus

        result shouldBe EclSubscriptionStatus(NotSubscribed)
    }
  }

}
