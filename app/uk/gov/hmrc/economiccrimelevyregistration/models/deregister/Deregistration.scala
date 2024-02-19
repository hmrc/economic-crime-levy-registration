/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.models.deregister

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.DeRegistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Registration, RegistrationType}

import java.time.{Instant, LocalDate}

case class Deregistration(
  internalId: String,
  eclReference: Option[String],
  reason: Option[DeregisterReason],
  date: Option[LocalDate],
  contactDetails: ContactDetails,
  registrationType: RegistrationType,
  lastUpdated: Option[Instant] = None
)

object Deregistration {
  implicit val format: OFormat[Deregistration] = Json.format[Deregistration]

  def empty(internalId: String): Deregistration = Deregistration(
    internalId = internalId,
    eclReference = None,
    reason = None,
    date = None,
    contactDetails = ContactDetails(None, None, None, None),
    DeRegistration
  )
}
