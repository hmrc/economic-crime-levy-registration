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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.time.TaxYear

import java.time.{Instant, LocalDate}

case class RegistrationAdditionalInfo(
  internalId: String,
  liabilityYear: Option[Int],
  eclReference: Option[String],
  liabilityStartDate: Option[LocalDate],
  registeringForCurrentYear: Option[Boolean],
  liableForPreviousYears: Option[Boolean],
  lastUpdated: Option[Instant]
) {

  val StartOfEclFinancialYear: String = {
    liabilityYear match {
      case Some(year) =>
        s"$year-04-01"
      case None       => s"${TaxYear.current.currentYear}-04-01"
    }
  }
}

object RegistrationAdditionalInfo {

  implicit val format: OFormat[RegistrationAdditionalInfo] = Json.format[RegistrationAdditionalInfo]

  def empty(internalId: String): RegistrationAdditionalInfo =
    RegistrationAdditionalInfo(
      internalId = internalId,
      liabilityYear = None,
      eclReference = None,
      liabilityStartDate = None,
      registeringForCurrentYear = None,
      liableForPreviousYears = None,
      lastUpdated = None
    )
}
