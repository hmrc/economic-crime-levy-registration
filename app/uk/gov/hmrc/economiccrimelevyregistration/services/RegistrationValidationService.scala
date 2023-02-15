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
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.LegalEntityDetails.CustomerType
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, ContactDetails, EclAddress, Registration}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId}
import javax.inject.Inject

class RegistrationValidationService @Inject() (clock: Clock) {

  type ValidationResult[A] = ValidatedNec[DataValidationError, A]

  def validateRegistration(registration: Registration): ValidationResult[EclSubscription] =
    (
      validateLegalEntityDetails(registration),
      validateAmlSupervisor(registration),
      validateOptExists(registration.businessSector, "Business sector"),
      validateFirstContactDetails(registration.contacts.firstContactDetails),
      validateSecondContactDetails(registration),
      validateEclAddress(registration.contactAddress),
      validateAmlRegulatedActivity(registration),
      validateOptExists(registration.relevantAp12Months, "Relevant AP 12 months choice"),
      validateOptExists(registration.relevantApRevenue, "Relevant AP revenue"),
      validateConditionalOptExists(
        registration.relevantApLength,
        registration.relevantAp12Months.contains(false),
        "Relevant AP length"
      ),
      validateRevenueMeetsThreshold(registration)
    ).mapN {
      (
        legalEntityDetails,
        amlSupervisor,
        businessSector,
        firstContactDetails,
        secondContactDetails,
        contactAddress,
        _,
        _,
        _,
        _,
        _
      ) =>
        val eclSubscription = EclSubscription(
          legalEntityDetails = legalEntityDetails(amlSupervisor, businessSector.toString),
          correspondenceAddressDetails = contactAddress,
          primaryContactDetails = firstContactDetails,
          secondaryContactDetails = secondContactDetails
        )

        eclSubscription
    }

  private def validateFirstContactDetails(details: ContactDetails): ValidationResult[SubscriptionContactDetails] =
    (
      validateOptExists(details.name, "First contact name"),
      validateOptExists(details.role, "First contact role"),
      validateOptExists(
        details.emailAddress,
        "First contact email"
      ),
      validateOptExists(
        details.telephoneNumber,
        "First contact number"
      )
    ).mapN { (name, role, email, number) =>
      SubscriptionContactDetails(name = name, positionInCompany = role, telephone = number, emailAddress = email)
    }

  private def validateSecondContactDetails(
    registration: Registration
  ): ValidationResult[Option[SubscriptionContactDetails]] =
    registration.contacts.secondContact match {
      case Some(true)  =>
        (
          validateOptExists(
            registration.contacts.secondContactDetails.name,
            "Second contact name"
          ),
          validateOptExists(
            registration.contacts.secondContactDetails.role,
            "Second contact role"
          ),
          validateOptExists(
            registration.contacts.secondContactDetails.emailAddress,
            "Second contact email"
          ),
          validateOptExists(
            registration.contacts.secondContactDetails.telephoneNumber,
            "Second contact number"
          )
        ).mapN((name, role, email, number) =>
          Some(
            SubscriptionContactDetails(
              name = name,
              positionInCompany = role,
              telephone = number,
              emailAddress = email
            )
          )
        )
      case Some(false) => None.validNec
      case _           => DataValidationError(DataMissing, missingErrorMessage("Second contact choice")).invalidNec
    }

  private def validateLegalEntityDetails(
    registration: Registration
  ): ValidationResult[(String, String) => LegalEntityDetails] = {
    val grsJourneyData: (
      Option[IncorporatedEntityJourneyData],
      Option[PartnershipEntityJourneyData],
      Option[SoleTraderEntityJourneyData]
    ) = (
      registration.incorporatedEntityJourneyData,
      registration.partnershipEntityJourneyData,
      registration.soleTraderEntityJourneyData
    )

    def registrationDate: String =
      LocalDate.ofInstant(Instant.now(clock), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)

    registration.entityType match {
      case Some(UkLimitedCompany)                                                              =>
        grsJourneyData match {
          case (Some(i), None, None) =>
            validateOptExists(i.registration.registeredBusinessPartnerId, "Business partner ID").map {
              businessPartnerId =>
                LegalEntityDetails(
                  safeId = businessPartnerId,
                  customerIdentification1 = i.ctutr,
                  customerIdentification2 = Some(i.companyProfile.companyNumber),
                  organisationName = Some(i.companyProfile.companyName),
                  firstName = None,
                  lastName = None,
                  customerType = CustomerType.Organisation,
                  registrationDate = registrationDate,
                  _,
                  _
                )
            }
          case _                     => DataValidationError(DataMissing, missingErrorMessage("Incorporated entity data")).invalidNec
        }
      case Some(LimitedLiabilityPartnership | LimitedPartnership | ScottishLimitedPartnership) =>
        grsJourneyData match {
          case (None, Some(lp), None) =>
            (
              validateOptExists(lp.registration.registeredBusinessPartnerId, "Business partner ID"),
              validateOptExists(lp.sautr, "Partnership SA UTR"),
              validateOptExists(lp.companyProfile, "Partnership company profile")
            ).mapN { (businessPartnerId, sautr, companyProfile) =>
              LegalEntityDetails(
                safeId = businessPartnerId,
                customerIdentification1 = sautr,
                customerIdentification2 = Some(companyProfile.companyNumber),
                organisationName = Some(companyProfile.companyName),
                firstName = None,
                lastName = None,
                customerType = CustomerType.Organisation,
                registrationDate = registrationDate,
                _,
                _
              )
            }
          case _                      => DataValidationError(DataMissing, missingErrorMessage("Partnership data")).invalidNec
        }
      case Some(GeneralPartnership | ScottishPartnership)                                      =>
        grsJourneyData match {
          case (None, Some(p), None) =>
            (
              validateOptExists(p.registration.registeredBusinessPartnerId, "Business partner ID"),
              validateOptExists(p.sautr, "Partnership SA UTR"),
              validateOptExists(p.postcode, "Partnership postcode"),
              validateOptExists(registration.partnershipName, "Partnership name")
            ).mapN { (businessPartnerId, sautr, postcode, partnershipName) =>
              LegalEntityDetails(
                safeId = businessPartnerId,
                customerIdentification1 = sautr,
                customerIdentification2 = Some(postcode),
                organisationName = Some(partnershipName),
                firstName = None,
                lastName = None,
                customerType = CustomerType.Organisation,
                registrationDate = registrationDate,
                _,
                _
              )
            }
          case _                     => DataValidationError(DataMissing, missingErrorMessage("Partnership data")).invalidNec
        }
      case Some(SoleTrader)                                                                    =>
        grsJourneyData match {
          case (None, None, Some(s)) =>
            (
              validateOptExists(s.registration.registeredBusinessPartnerId, "Business partner ID"),
              validateSoleTraderIdentifiers(s)
            ).mapN { (businessPartnerId, soleTraderIdentifiers) =>
              LegalEntityDetails(
                safeId = businessPartnerId,
                customerIdentification1 = soleTraderIdentifiers._1,
                customerIdentification2 = soleTraderIdentifiers._2,
                organisationName = None,
                firstName = Some(s.fullName.firstName),
                lastName = Some(s.fullName.lastName),
                customerType = CustomerType.Individual,
                registrationDate = registrationDate,
                _,
                _
              )
            }
          case _                     => DataValidationError(DataMissing, missingErrorMessage("Sole trader data")).invalidNec
        }
      case _                                                                                   =>
        DataValidationError(DataMissing, missingErrorMessage("Entity type")).invalidNec
    }

  }

  private def validateSoleTraderIdentifiers(
    s: SoleTraderEntityJourneyData
  ): ValidationResult[(String, Option[String])] =
    (s.sautr, s.nino) match {
      case (Some(sautr), Some(nino)) => (sautr, Some(nino)).validNec
      case (Some(sautr), _)          => (sautr, None).validNec
      case (_, Some(nino))           => (nino, None).validNec
      case _                         => DataValidationError(DataMissing, missingErrorMessage("Sole trader SA UTR or NINO")).invalidNec
    }

  private def validateAmlRegulatedActivity(registration: Registration): ValidationResult[Registration] =
    registration.carriedOutAmlRegulatedActivityInCurrentFy match {
      case Some(true)  => registration.validNec
      case Some(false) =>
        DataValidationError(DataInvalid, "Carried out AML regulated activity cannot be false").invalidNec
      case _           =>
        DataValidationError(DataMissing, missingErrorMessage("Carried out AML regulated activity choice")).invalidNec
    }

  private def validateAmlSupervisor(registration: Registration): ValidationResult[String] =
    registration.amlSupervisor match {
      case Some(AmlSupervisor(GamblingCommission | FinancialConductAuthority, _)) =>
        DataValidationError(DataInvalid, "AML supervisor cannot be GC or FCA").invalidNec
      case Some(AmlSupervisor(Hmrc, _))                                           => Hmrc.toString.validNec
      case Some(AmlSupervisor(_, Some(otherProfessionalBody)))                    => otherProfessionalBody.validNec
      case _                                                                      =>
        DataValidationError(DataMissing, missingErrorMessage("AML supervisor")).invalidNec
    }

  private def validateRevenueMeetsThreshold(registration: Registration): ValidationResult[Registration] =
    registration.revenueMeetsThreshold match {
      case Some(true)  => registration.validNec
      case Some(false) => DataValidationError(DataInvalid, "Revenue does not meet the liability threshold").invalidNec
      case _           => DataValidationError(DataMissing, missingErrorMessage("Revenue meets threshold flag")).invalidNec
    }

  private def validateEclAddress(eclAddress: Option[EclAddress]): ValidationResult[CorrespondenceAddressDetails] =
    eclAddress match {
      case Some(address) =>
        val addressLines: Seq[String] = Seq(
          address.organisation,
          address.addressLine1,
          address.addressLine2,
          address.addressLine3,
          address.addressLine4,
          address.region,
          address.poBox
        ).flatMap(_.toSeq)

        addressLines match {
          case line1 :: otherLines =>
            CorrespondenceAddressDetails(
              line1,
              otherLines,
              address.postCode,
              address.countryCode
            ).validNec
          case _                   => DataValidationError(DataInvalid, "Contact address has no address lines").invalidNec
        }
      case _             => DataValidationError(DataMissing, missingErrorMessage("Contact address")).invalidNec
    }

  private def validateOptExists[T](optData: Option[T], description: String): ValidationResult[T] =
    optData match {
      case Some(value) => value.validNec
      case _           => DataValidationError(DataMissing, missingErrorMessage(description)).invalidNec
    }

  private def validateConditionalOptExists[T](
    optData: Option[T],
    condition: Boolean,
    description: String
  ): ValidationResult[Option[T]] =
    if (condition) {
      optData match {
        case Some(value) => Some(value).validNec
        case _           => DataValidationError(DataMissing, missingErrorMessage(description)).invalidNec
      }
    } else {
      None.validNec
    }

  private def missingErrorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

}
