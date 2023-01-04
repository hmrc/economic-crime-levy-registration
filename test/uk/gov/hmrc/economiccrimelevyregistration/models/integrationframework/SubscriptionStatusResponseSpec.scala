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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.Arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.Channel._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EtmpSubscriptionStatus._

class SubscriptionStatusResponseSpec extends SpecBase {

  "toEclSubscriptionStatus" should {
    "return Subscribed with the ECL registration reference when the id type is ZECL" in forAll {
      (idValue: String, channel: Option[Channel]) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = Successful,
          idType = Some("ZECL"),
          idValue = Some(idValue),
          channel = channel
        )

        val result = subscriptionStatusResponse.toEclSubscriptionStatus

        result shouldBe EclSubscriptionStatus(Subscribed(idValue))
    }

    "return NotSubscribed when the subscription status is not Successful and there is no id type or value" in forAll(
      Arbitrary.arbitrary[EtmpSubscriptionStatus].retryUntil(_ != Successful),
      Arbitrary.arbitrary[Option[Channel]]
    ) {
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

    "throw an IllegalStateException when the id type something other than ZECL" in forAll(
      Arbitrary.arbitrary[String].retryUntil(_ != "ZECL"),
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[Option[Channel]],
      Arbitrary.arbitrary[EtmpSubscriptionStatus]
    ) {
      (
        idType: String,
        idValue: String,
        channel: Option[Channel],
        subscriptionStatus: EtmpSubscriptionStatus
      ) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = subscriptionStatus,
          idType = Some(idType),
          idValue = Some(idValue),
          channel = channel
        )

        val result = intercept[IllegalStateException] {
          subscriptionStatusResponse.toEclSubscriptionStatus
        }

        result.getMessage shouldBe s"Subscription status $subscriptionStatus returned with unexpected idType $idType and value $idValue"
    }

    "throw an IllegalStateException when the subscription status is successful but there is no id type or value" in forAll(
      Table(
        ("idType", "idValue"),
        (None, None),
        (Some("ZECL"), None),
        (None, Some(testEclRegistrationReference))
      )
    ) {
      (
        idType: Option[String],
        idValue: Option[String]
      ) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = Successful,
          idType = idType,
          idValue = idValue,
          channel = Some(Online)
        )

        val result = intercept[IllegalStateException] {
          subscriptionStatusResponse.toEclSubscriptionStatus
        }

        result.getMessage shouldBe "Subscription status is Successful but there is no id type or value"
    }
  }

}
