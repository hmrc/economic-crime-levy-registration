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

import play.api.libs.json.{Json, OFormat}

final case class EclSubscription(
  legalEntityDetails: LegalEntityDetails,
  correspondenceAddressDetails: CorrespondenceAddressDetails,
  primaryContactDetails: SubscriptionContactDetails,
  secondaryContactDetails: Option[SubscriptionContactDetails]
)

object EclSubscription {
  implicit val format: OFormat[EclSubscription] = Json.format[EclSubscription]
}

final case class LegalEntityDetails(
  safeId: String,
  customerIdentification1: String,
  customerIdentification2: Option[String],
  organisationName: Option[String],
  firstName: Option[String],
  lastName: Option[String],
  customerType: String,
  registrationDate: String,
  amlSupervisor: String,
  businessSector: String
)

object LegalEntityDetails {
  object CustomerType {
    val Organisation = "01"
    val Individual   = "02"
  }

  implicit val format: OFormat[LegalEntityDetails] = Json.format[LegalEntityDetails]
}

final case class CorrespondenceAddressDetails(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postCode: Option[String],
  country: Option[String]
)

object CorrespondenceAddressDetails {
  def apply(
    line1: String,
    otherLines: Seq[String],
    postCode: Option[String],
    countryCode: String
  ): CorrespondenceAddressDetails = {
    val groupedLines = (line1 +: otherLines).grouped(2).toSeq

    CorrespondenceAddressDetails(
      addressLine1 = groupedLines.headOption.map(_.mkString(", ")).getOrElse(line1),
      addressLine2 = groupedLines.lift(1).map(_.mkString(", ")),
      addressLine3 = groupedLines.lift(2).map(_.mkString(", ")),
      addressLine4 = groupedLines.lift(3).map(_.mkString(", ")),
      postCode = postCode,
      country = Some(countryCode)
    )
  }

  implicit val format: OFormat[CorrespondenceAddressDetails] = Json.format[CorrespondenceAddressDetails]
}

final case class SubscriptionContactDetails(
  name: String,
  positionInCompany: String,
  telephone: String,
  emailAddress: String
)

object SubscriptionContactDetails {
  implicit val format: OFormat[SubscriptionContactDetails] = Json.format[SubscriptionContactDetails]
}
