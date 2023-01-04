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

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EtmpSubscriptionStatus._

class EtmpSubscriptionStatusSpec extends SpecBase {

  "reads" should {
    "return the subscription status deserialized from its JSON representation" in forAll(
      Table(
        ("subscriptionStatus", "expectedResult"),
        ("NO_FORM_BUNDLE_FOUND", NoFormBundleFound),
        ("REG_FORM_RECEIVED", RegFormReceived),
        ("SENT_TO_DS", SentToDs),
        ("DS_OUTCOME_IN_PROGRESS", DsOutcomeInProgress),
        ("SUCCESSFUL", Successful),
        ("REJECTED", Rejected),
        ("IN_PROCESSING", InProcessing),
        ("CREATE_FAILED", CreateFailed),
        ("WITHDRAWAL", Withdrawal),
        ("SENT_TO_RCM", SentToRcm),
        ("APPROVED_WITH_CONDITIONS", ApprovedWithConditions),
        ("REVOKED", Revoked),
        ("DE-REGISTERED", DeRegistered),
        ("CONTRACT_OBJECT_INACTIVE", ContractObjectInactive)
      )
    ) { (subscriptionStatus: String, expectedResult: EtmpSubscriptionStatus) =>
      val json = JsString(subscriptionStatus)

      json.as[EtmpSubscriptionStatus] shouldBe expectedResult
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[EtmpSubscriptionStatus](JsString("Test"))

      result shouldBe JsError("Test is not a valid SubscriptionStatus")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[EtmpSubscriptionStatus](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
