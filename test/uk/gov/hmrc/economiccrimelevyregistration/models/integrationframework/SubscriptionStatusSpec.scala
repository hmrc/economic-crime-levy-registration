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

package uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary

class SubscriptionStatusSpec extends SpecBase {
  "writes" should {
    "return the subscription status serialized to its JSON representation" in forAll(
      Table(
        ("subscriptionStatus", "expectedResult"),
        (NoFormBundleFound, "NO_FORM_BUNDLE_FOUND"),
        (RegFormReceived, "REG_FORM_RECEIVED"),
        (SentToDs, "SENT_TO_DS"),
        (DsOutcomeInProgress, "DS_OUTCOME_IN_PROGRESS"),
        (Successful, "SUCCESSFUL"),
        (Rejected, "REJECTED"),
        (InProcessing, "IN_PROCESSING"),
        (CreateFailed, "CREATE_FAILED"),
        (Withdrawal, "WITHDRAWAL"),
        (SentToRcm, "SENT_TO_RCM"),
        (ApprovedWithConditions, "APPROVED_WITH_CONDITIONS"),
        (Revoked, "REVOKED"),
        (DeRegistered, "DE-REGISTERED"),
        (ContractObjectInactive, "CONTRACT_OBJECT_INACTIVE")
      )
    ) { (subscriptionStatus: SubscriptionStatus, expectedResult: String) =>
      val result = Json.toJson(subscriptionStatus)

      result shouldBe JsString(expectedResult)
    }
  }

  "reads" should {
    "return the subscription status deserialized from its JSON representation" in forAll {
      (subscriptionStatus: SubscriptionStatus) =>
        val json = Json.toJson(subscriptionStatus)

        json.as[SubscriptionStatus] shouldBe subscriptionStatus
    }

    "return a '... is not a valid SubscriptionStatus' error when passed an invalid string value" in forAll {
      (value: String) =>
        val result = Json.fromJson[SubscriptionStatus](JsString(value))

        result shouldBe JsError(s"$value is not a valid SubscriptionStatus")
    }

    "raise an error when passed a type that is not a string" in {
      val result = Json.fromJson[SubscriptionStatus](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
