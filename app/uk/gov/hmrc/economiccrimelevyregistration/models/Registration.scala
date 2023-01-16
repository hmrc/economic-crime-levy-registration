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
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}

import java.time.Instant

final case class Registration(
  internalId: String,
  carriedOutAmlRegulatedActivityInCurrentFy: Option[Boolean],
  amlSupervisor: Option[AmlSupervisor],
  entityType: Option[EntityType],
  incorporatedEntityJourneyData: Option[IncorporatedEntityJourneyData],
  soleTraderEntityJourneyData: Option[SoleTraderEntityJourneyData],
  partnershipEntityJourneyData: Option[PartnershipEntityJourneyData],
  businessSector: Option[BusinessSector],
  contacts: Contacts,
  useRegisteredOfficeAddressAsContactAddress: Option[Boolean],
  contactAddressIsUk: Option[Boolean],
  contactAddress: Option[EclAddress],
  lastUpdated: Option[Instant] = None
)

object Registration {
  implicit val format: OFormat[Registration] = Json.format[Registration]

  def empty(internalId: String): Registration = Registration(
    internalId = internalId,
    carriedOutAmlRegulatedActivityInCurrentFy = None,
    entityType = None,
    amlSupervisor = None,
    incorporatedEntityJourneyData = None,
    soleTraderEntityJourneyData = None,
    partnershipEntityJourneyData = None,
    businessSector = None,
    contacts = Contacts.empty,
    useRegisteredOfficeAddressAsContactAddress = None,
    contactAddress = None,
    contactAddressIsUk = None
  )
}
