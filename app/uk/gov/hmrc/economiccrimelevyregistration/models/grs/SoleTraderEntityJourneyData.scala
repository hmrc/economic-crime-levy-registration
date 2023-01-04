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

package uk.gov.hmrc.economiccrimelevyregistration.models.grs

import play.api.libs.json.{Json, OFormat}

import java.util.Date

final case class SoleTraderEntityJourneyData(
  fullName: FullName,
  dateOfBirth: Date,
  nino: Option[String],
  sautr: Option[String],
  identifiersMatch: Boolean,
  businessVerification: Option[BusinessVerificationResult],
  registration: GrsRegistrationResult
)

object SoleTraderEntityJourneyData {
  implicit val format: OFormat[SoleTraderEntityJourneyData] =
    Json.format[SoleTraderEntityJourneyData]
}

final case class FullName(
  firstName: String,
  lastName: String
)

object FullName {
  implicit val format: OFormat[FullName] =
    Json.format[FullName]
}
