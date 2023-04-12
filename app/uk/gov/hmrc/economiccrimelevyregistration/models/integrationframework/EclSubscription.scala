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
  businessPartnerId: String,
  subscription: Subscription
)

object EclSubscription {
  implicit val format: OFormat[EclSubscription] = Json.format[EclSubscription]
}

final case class Subscription(
  legalEntityDetails: LegalEntityDetails,
  correspondenceAddressDetails: CorrespondenceAddressDetails,
  primaryContactDetails: SubscriptionContactDetails,
  secondaryContactDetails: Option[SubscriptionContactDetails]
)

object Subscription {
  implicit val format: OFormat[Subscription] = Json.format[Subscription]
}

final case class LegalEntityDetails(
  customerIdentification1: String,
  customerIdentification2: Option[String],
  organisationName: Option[String],
  firstName: Option[String],
  lastName: Option[String],
  customerType: String,
  registrationDate: String,
  liabilityStartDate: String,
  amlSupervisor: String,
  businessSector: String
)

object LegalEntityDetails {
  object CustomerType {
    val Individual   = "01"
    val Organisation = "02"
  }

  val StartOfFirstEclFinancialYear = "2022-04-01"

  implicit val format: OFormat[LegalEntityDetails] = Json.format[LegalEntityDetails]
}

final case class CorrespondenceAddressDetails(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postCode: Option[String],
  countryCode: Option[String]
)

object CorrespondenceAddressDetails {
  def apply(
    line1: String,
    otherLines: Seq[String],
    postCode: Option[String],
    countryCode: String
  ): CorrespondenceAddressDetails = {
    val AddressLineMaxLength = 35

    def seqOptLinesToString(seq: Seq[Option[String]]): Option[String] =
      Option(seq.flatten.mkString(", ")).filter(_.nonEmpty)

    def overflow(optS: Option[String]): Option[String] =
      optS.map(s => s.takeRight(Math.max(0, s.length - AddressLineMaxLength))).filter(_.nonEmpty)

    val addressLine1 = line1
    val addressLine2 = seqOptLinesToString(Seq(overflow(Some(line1)), otherLines.headOption, otherLines.lift(3)))
    val addressLine3 = seqOptLinesToString(Seq(overflow(addressLine2), otherLines.lift(1), otherLines.lift(4)))
    val addressLine4 = seqOptLinesToString(Seq(overflow(addressLine3), otherLines.lift(2), otherLines.lift(5)))

    CorrespondenceAddressDetails(
      addressLine1 = addressLine1.take(AddressLineMaxLength),
      addressLine2 = addressLine2.map(_.take(AddressLineMaxLength)),
      addressLine3 = addressLine3.map(_.take(AddressLineMaxLength)),
      addressLine4 = addressLine4.map(_.take(AddressLineMaxLength)),
      postCode = postCode,
      countryCode = Some(countryCode)
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
