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

package uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework

import play.api.libs.json.{Json, OFormat}

case class GetSubscriptionResponse(
  processingDateTime: String,
  legalEntityDetails: GetLegalEntityDetails,
  correspondenceAddressDetails: GetCorrespondenceAddressDetails,
  primaryContactDetails: GetPrimaryContactDetails,
  secondaryContactDetails: Option[GetSecondaryContactDetails],
  additionalDetails: GetAdditionalDetails
)
object GetSubscriptionResponse {
  implicit val format: OFormat[GetSubscriptionResponse] = Json.format[GetSubscriptionResponse]
}

case class GetLegalEntityDetails(
  customerIdentification1: String,
  customerIdentification2: Option[String],
  customerType: String,
  organisationName: Option[String] = None,
  firstName: Option[String] = None,
  lastName: Option[String] = None
)
object GetLegalEntityDetails {
  implicit val format: OFormat[GetLegalEntityDetails] = Json.format[GetLegalEntityDetails]
}

case class GetCorrespondenceAddressDetails(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postCode: Option[String],
  countryCode: Option[String]
)
object GetCorrespondenceAddressDetails {
  implicit val format: OFormat[GetCorrespondenceAddressDetails] = Json.format[GetCorrespondenceAddressDetails]
}

case class GetPrimaryContactDetails(name: String, positionInCompany: String, telephone: String, emailAddress: String)

object GetPrimaryContactDetails {
  implicit val format: OFormat[GetPrimaryContactDetails] = Json.format[GetPrimaryContactDetails]
}

case class GetSecondaryContactDetails(name: String, positionInCompany: String, telephone: String, emailAddress: String)

object GetSecondaryContactDetails {
  implicit val format: OFormat[GetSecondaryContactDetails] = Json.format[GetSecondaryContactDetails]
}

case class GetAdditionalDetails(
  registrationDate: String,
  liabilityStartDate: String,
  eclReference: String,
  amlSupervisor: String,
  businessSector: String
)
object GetAdditionalDetails {
  implicit val format: OFormat[GetAdditionalDetails] = Json.format[GetAdditionalDetails]

}
