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

package uk.gov.hmrc.economiccrimelevyregistration.services

import cats.data.ValidatedNec
import cats.implicits._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}

import javax.inject.Inject

class RegistrationValidationService @Inject() () {

  type ValidationResult[A] = ValidatedNec[DataValidationError, A]

  def validateRegistration(registration: Registration): ValidationResult[String] =
    (
      validateGrsJourneyData(registration),
      validateOptExists(
        registration.carriedOutAmlRegulatedActivityInCurrentFy,
        missingErrorMessage("Carried out AML regulated activity choice")
      ),
      validateOptExists(registration.relevantAp12Months, missingErrorMessage("Relevant AP 12 months choice")),
      validateOptExists(registration.relevantApRevenue, missingErrorMessage("Relevant AP revenue")),
      validateConditionalOptExists(
        registration.relevantApLength,
        registration.relevantAp12Months.contains(false),
        missingErrorMessage("Relevant AP length")
      ),
      validateOptExists(registration.contacts.firstContactDetails.name, missingErrorMessage("First contact name")),
      validateOptExists(registration.contacts.firstContactDetails.role, missingErrorMessage("First contact role")),
      validateOptExists(
        registration.contacts.firstContactDetails.emailAddress,
        missingErrorMessage("First contact email")
      ),
      validateOptExists(
        registration.contacts.firstContactDetails.telephoneNumber,
        missingErrorMessage("First contact number")
      ),
      validateOptExists(registration.businessSector, missingErrorMessage("Business sector")),
      validateOptExists(registration.contactAddress, missingErrorMessage("Contact address")),
      validateOptExists(registration.amlSupervisor, missingErrorMessage("AML supervisor")),
      validateSecondContactDetails(registration)
    ).mapN((_, _, _, _, _, _, _, _, _, _, _, _, _) => registration)
    ).mapN((businessPartnerId, _, _, _, _, _, _, _, _) => businessPartnerId)

  private def validateSecondContactDetails(registration: Registration): ValidationResult[Registration] =
    registration.contacts.secondContact match {
      case Some(true)  =>
        (
          validateOptExists(
            registration.contacts.secondContactDetails.name,
            missingErrorMessage("Second contact name")
          ),
          validateOptExists(
            registration.contacts.secondContactDetails.role,
            missingErrorMessage("Second contact role")
          ),
          validateOptExists(
            registration.contacts.secondContactDetails.emailAddress,
            missingErrorMessage("Second contact email")
          ),
          validateOptExists(
            registration.contacts.secondContactDetails.telephoneNumber,
            missingErrorMessage("Second contact number")
          )
        ).mapN((_, _, _, _) => registration)
      case Some(false) => registration.validNec
      case _           => DataValidationError(missingErrorMessage("Second contact choice")).invalidNec
    }

  private def validateGrsJourneyData(registration: Registration): ValidationResult[String] = {
    val grsJourneyData: (
      Option[IncorporatedEntityJourneyData],
      Option[PartnershipEntityJourneyData],
      Option[SoleTraderEntityJourneyData]
    ) = (
      registration.incorporatedEntityJourneyData,
      registration.partnershipEntityJourneyData,
      registration.soleTraderEntityJourneyData
    )

    val validateBusinessPartnerId: Option[String] => ValidationResult[String] = {
      case Some(s) => s.validNec
      case _       => DataValidationError(missingErrorMessage("Business partner ID")).invalidNec
    }

    registration.entityType match {
      case Some(UkLimitedCompany) =>
        grsJourneyData match {
          case (Some(i), None, None) => validateBusinessPartnerId(i.registration.registeredBusinessPartnerId)
          case _                     => DataValidationError(missingErrorMessage("Incorporated entity data")).invalidNec
        }
      case Some(
            LimitedLiabilityPartnership | GeneralPartnership | ScottishPartnership | LimitedPartnership |
            ScottishLimitedPartnership
          ) =>
        grsJourneyData match {
          case (None, Some(p), None) => validateBusinessPartnerId(p.registration.registeredBusinessPartnerId)
          case _                     => DataValidationError(missingErrorMessage("Partnership data")).invalidNec
        }
      case Some(SoleTrader)       =>
        grsJourneyData match {
          case (None, None, Some(s)) => validateBusinessPartnerId(s.registration.registeredBusinessPartnerId)
          case _                     => DataValidationError(missingErrorMessage("Sole trader data")).invalidNec
        }
      case _                      => DataValidationError(missingErrorMessage("Entity type")).invalidNec
    }
  }

  private def missingErrorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

  private def validateOptExists[T](optData: Option[T], description: String): ValidationResult[T] =
    optData match {
      case Some(value) => value.validNec
      case _           => DataValidationError(description).invalidNec
    }

  private def validateConditionalOptExists[T](
    optData: Option[T],
    condition: Boolean,
    description: String
  ): ValidationResult[Option[T]] =
    if (condition) {
      optData match {
        case Some(value) => Some(value).validNec
        case _           => DataValidationError(description).invalidNec
      }
    } else {
      None.validNec
    }

}
