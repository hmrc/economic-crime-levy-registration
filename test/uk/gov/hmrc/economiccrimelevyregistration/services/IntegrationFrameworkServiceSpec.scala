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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.IntegrationFrameworkConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, NotSubscribed, Subscribed}

import scala.concurrent.Future

class IntegrationFrameworkServiceSpec extends SpecBase {
  val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]

  val service = new IntegrationFrameworkService(mockIntegrationFrameworkConnector)

  "getSubscriptionStatus" should {
    "return Subscribed with the ECL registration reference when the id type is ZECL" in forAll {
      (businessPartnerId: String, idValue: String, channel: Option[Channel]) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = Successful,
          idType = Some("ZECL"),
          idValue = Some(idValue),
          channel = channel
        )

        when(mockIntegrationFrameworkConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any()))
          .thenReturn(Future.successful(subscriptionStatusResponse))

        val result = await(service.getSubscriptionStatus(businessPartnerId))

        result shouldBe EclSubscriptionStatus(Subscribed(idValue))
    }

    "return NotSubscribed when the subscription status is not Successful and there is no id type or value" in forAll(
      Arbitrary.arbitrary[EtmpSubscriptionStatus].retryUntil(_ != Successful),
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[Option[Channel]]
    ) {
      (
        subscriptionStatus: EtmpSubscriptionStatus,
        businessPartnerId: String,
        idValue: String,
        channel: Option[Channel]
      ) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = subscriptionStatus,
          idType = None,
          idValue = None,
          channel = channel
        )

        when(mockIntegrationFrameworkConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any()))
          .thenReturn(Future.successful(subscriptionStatusResponse))

        val result = await(service.getSubscriptionStatus(businessPartnerId))

        result shouldBe EclSubscriptionStatus(NotSubscribed)
    }

    "throw an IllegalStateException when the id type something other than ZECL" in forAll(
      Arbitrary.arbitrary[String].retryUntil(_ != "ZECL"),
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[String],
      Arbitrary.arbitrary[Option[Channel]],
      Arbitrary.arbitrary[EtmpSubscriptionStatus]
    ) {
      (
        idType: String,
        businessPartnerId: String,
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

        when(mockIntegrationFrameworkConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any()))
          .thenReturn(Future.successful(subscriptionStatusResponse))

        val result = intercept[IllegalStateException] {
          await(service.getSubscriptionStatus(businessPartnerId))
        }

        result.getMessage shouldBe s"Subscription status $subscriptionStatus returned with unexpected idType $idType and value $idValue"
    }

    "throw an IllegalStateException when the subscription status is successful but there is no id type or value" in forAll {
      (
        businessPartnerId: String,
        channel: Option[Channel]
      ) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = Successful,
          idType = None,
          idValue = None,
          channel = channel
        )

        when(mockIntegrationFrameworkConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any()))
          .thenReturn(Future.successful(subscriptionStatusResponse))

        val result = intercept[IllegalStateException] {
          await(service.getSubscriptionStatus(businessPartnerId))
        }

        result.getMessage shouldBe s"Subscription status is ${subscriptionStatusResponse.subscriptionStatus} but there is no id type or value"
    }

    "throw an IllegalStateException when the subscription status is successful but there is an id type without an id value" in forAll {
      (
        businessPartnerId: String,
        channel: Option[Channel]
      ) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = Successful,
          idType = Some("ZECL"),
          idValue = None,
          channel = channel
        )

        when(mockIntegrationFrameworkConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any()))
          .thenReturn(Future.successful(subscriptionStatusResponse))

        val result = intercept[IllegalStateException] {
          await(service.getSubscriptionStatus(businessPartnerId))
        }

        result.getMessage shouldBe s"Subscription status is ${subscriptionStatusResponse.subscriptionStatus} but there is no id type or value"
    }

    "throw an IllegalStateException when the subscription status is successful but there is an id value without an id type" in forAll {
      (
        businessPartnerId: String,
        idValue: String,
        channel: Option[Channel]
      ) =>
        val subscriptionStatusResponse = SubscriptionStatusResponse(
          subscriptionStatus = Successful,
          idType = None,
          idValue = Some(idValue),
          channel = channel
        )

        when(mockIntegrationFrameworkConnector.getSubscriptionStatus(ArgumentMatchers.eq(businessPartnerId))(any()))
          .thenReturn(Future.successful(subscriptionStatusResponse))

        val result = intercept[IllegalStateException] {
          await(service.getSubscriptionStatus(businessPartnerId))
        }

        result.getMessage shouldBe s"Subscription status is ${subscriptionStatusResponse.subscriptionStatus} but there is no id type or value"
    }
  }

}
