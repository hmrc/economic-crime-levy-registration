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

package uk.gov.hmrc.economiccrimelevyregistration.models.grs

import play.api.libs.json.{Json, OFormat}

import java.util.Date

final case class IncorporatedEntityJourneyData(
  companyProfile: CompanyProfile,
  ctutr: String,
  identifiersMatch: Boolean,
  businessVerification: Option[BusinessVerificationResult],
  registration: GrsRegistrationResult
)

object IncorporatedEntityJourneyData {
  implicit val format: OFormat[IncorporatedEntityJourneyData] =
    Json.format[IncorporatedEntityJourneyData]
}

final case class CompanyProfile(
  companyName: String,
  companyNumber: String,
  dateOfIncorporation: Date,
  unsanitisedCHROAddress: IncorporatedEntityAddress
)

object CompanyProfile {
  implicit val format: OFormat[CompanyProfile] =
    Json.format[CompanyProfile]
}

final case class IncorporatedEntityAddress(
  address_line_1: String,
  address_line_2: String,
  care_of: String,
  country: String,
  locality: String,
  po_box: String,
  postal_code: String,
  premises: String,
  region: String
)

object IncorporatedEntityAddress {
  implicit val format: OFormat[IncorporatedEntityAddress] =
    Json.format[IncorporatedEntityAddress]
}
