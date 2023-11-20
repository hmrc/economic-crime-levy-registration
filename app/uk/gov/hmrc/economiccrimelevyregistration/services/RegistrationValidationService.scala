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

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.implicits._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.UtrType.{CtUtr, SaUtr}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.LegalEntityDetails.CustomerType
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.StringUtils._
import uk.gov.hmrc.economiccrimelevyregistration.utils.{SchemaLoader, SchemaValidator}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneOffset}
import javax.inject.Inject

class RegistrationValidationService @Inject() (clock: Clock, schemaValidator: SchemaValidator) {

  type ValidationResult[A] = ValidatedNel[DataValidationError, A]

  def validateRegistration(
    registration: Registration,
    registrationAdditionalInfo: RegistrationAdditionalInfo
  ): ValidationResult[Either[EclSubscription, Registration]] =
    (registration.entityType) match {
      case Some(value) if EntityType.isOther(value) => validateOtherEntity(registration)
      case _                                        =>
        registration.registrationType match {
          case Some(Amendment) => transformToAmendedEclSubscription(registration)
          case _               =>
            transformToEclSubscription(registration, registrationAdditionalInfo) match {
              case Valid(Left(eclSubscription)) =>
                schemaValidator
                  .validateAgainstJsonSchema(
                    eclSubscription.subscription,
                    SchemaLoader.loadSchema("create-ecl-subscription-request.json")
                  )
                  .map(_ => Left(eclSubscription))
              case Valid(Right(_))              =>
                DataValidationError(DataInvalid, "Data was not transformed into a valid ECL subscription").invalidNel
              case invalid                      => invalid
            }
        }

    }

  private def transformToEclSubscription(
    registration: Registration,
    registrationAdditionalInfo: RegistrationAdditionalInfo
  ): ValidationResult[Either[EclSubscription, Registration]] =
    (
      validateAmlRegulatedActivityCommonFields(registration),
      validateLegalEntityDetails(registration, registrationAdditionalInfo),
      validateBusinessPartnerId(registration),
      validateOptExists(registration.businessSector, "Business sector"),
      validateContactDetails("First", registration.contacts.firstContactDetails),
      validateSecondContactDetails(registration.contacts),
      validateEclAddress(registration.contactAddress)
    ).mapN {
      (
        _,
        legalEntityDetails,
        businessPartnerId,
        businessSector,
        firstContactDetails,
        secondContactDetails,
        contactAddress
      ) =>
        Left(
          EclSubscription(
            businessPartnerId = businessPartnerId,
            subscription = Subscription(
              legalEntityDetails = legalEntityDetails(
                registration.amlSupervisor
                  .map(_.professionalBody)
                  .getOrElse("Unknown"),
                businessSector.toString
              ),
              correspondenceAddressDetails = contactAddress,
              primaryContactDetails = firstContactDetails,
              secondaryContactDetails = secondContactDetails
            )
          )
        )
    }

  private def transformToAmendedEclSubscription(
    registration: Registration
  ): ValidationResult[Either[EclSubscription, Registration]] =
    (
      validateAmlSupervisor(registration),
      validateOptExists(registration.businessSector, "Business sector"),
      validateContactDetails("First", registration.contacts.firstContactDetails),
      validateSecondContactDetails(registration.contacts),
      validateEclAddress(registration.contactAddress)
    ).mapN((_, _, _, _, _) => Right(registration))

  private def validateContactDetails(
    firstOrSecond: String,
    details: ContactDetails
  ): ValidationResult[SubscriptionContactDetails] =
    (
      validateOptExists(details.name, s"$firstOrSecond contact name"),
      validateOptExists(details.role, s"$firstOrSecond contact role"),
      validateOptExists(
        details.emailAddress,
        s"$firstOrSecond contact email"
      ),
      validateOptExists(
        details.telephoneNumber,
        s"$firstOrSecond contact number"
      )
    ).mapN { (name, role, email, number) =>
      SubscriptionContactDetails(
        name = name,
        positionInCompany = role,
        telephone = number.removeWhitespace,
        emailAddress = email
      )
    }

  private def validateSecondContactDetails(
    contacts: Contacts
  ): ValidationResult[Option[SubscriptionContactDetails]] =
    contacts.secondContact match {
      case Some(true)  => validateContactDetails("Second", contacts.secondContactDetails).map(Some(_))
      case Some(false) => None.validNel
      case _           => DataValidationError(DataMissing, missingErrorMessage("Second contact choice")).invalidNel
    }

  private def validateBusinessPartnerId(registration: Registration): ValidationResult[String] = {
    val optBusinessPartnerId = registration.incorporatedEntityJourneyData
      .flatMap(_.registration.registeredBusinessPartnerId)
      .orElse(registration.partnershipEntityJourneyData.flatMap(_.registration.registeredBusinessPartnerId))
      .orElse(registration.soleTraderEntityJourneyData.flatMap(_.registration.registeredBusinessPartnerId))

    validateOptExists(optBusinessPartnerId, "Business partner ID")
  }

  private def validateLegalEntityDetails(
    registration: Registration,
    registrationAdditionalInfo: RegistrationAdditionalInfo
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
      DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(Instant.now(clock))

    registration.entityType match {
      case Some(UkLimitedCompany | UnlimitedCompany | RegisteredSociety)                       =>
        grsJourneyData match {
          case (Some(i), None, None) =>
            (
              LegalEntityDetails(
                customerIdentification1 = i.ctutr,
                customerIdentification2 = Some(i.companyProfile.companyNumber),
                organisationName = Some(i.companyProfile.companyName),
                firstName = None,
                lastName = None,
                customerType = CustomerType.Organisation,
                registrationDate = registrationDate,
                liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear,
                _,
                _
              )
            ).validNel
          case _                     => DataValidationError(DataMissing, missingErrorMessage("Incorporated entity data")).invalidNel
        }
      case Some(LimitedLiabilityPartnership | LimitedPartnership | ScottishLimitedPartnership) =>
        grsJourneyData match {
          case (None, Some(lp), None) =>
            (
              validateOptExists(lp.sautr, "Partnership SA UTR"),
              validateOptExists(lp.companyProfile, "Partnership company profile")
            ).mapN { (sautr, companyProfile) =>
              LegalEntityDetails(
                customerIdentification1 = sautr,
                customerIdentification2 = Some(companyProfile.companyNumber),
                organisationName = Some(companyProfile.companyName),
                firstName = None,
                lastName = None,
                customerType = CustomerType.Organisation,
                registrationDate = registrationDate,
                liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear,
                _,
                _
              )
            }
          case _                      => DataValidationError(DataMissing, missingErrorMessage("Partnership data")).invalidNel
        }
      case Some(GeneralPartnership | ScottishPartnership)                                      =>
        grsJourneyData match {
          case (None, Some(p), None) =>
            (
              validateOptExists(p.sautr, "Partnership SA UTR"),
              validateOptExists(p.postcode.map(_.removeWhitespace), "Partnership postcode"),
              validateOptExists(registration.partnershipName, "Partnership name")
            ).mapN { (sautr, postcode, partnershipName) =>
              LegalEntityDetails(
                customerIdentification1 = sautr,
                customerIdentification2 = Some(postcode),
                organisationName = Some(partnershipName),
                firstName = None,
                lastName = None,
                customerType = CustomerType.Organisation,
                registrationDate = registrationDate,
                liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear,
                _,
                _
              )
            }
          case _                     => DataValidationError(DataMissing, missingErrorMessage("Partnership data")).invalidNel
        }
      case Some(SoleTrader)                                                                    =>
        grsJourneyData match {
          case (None, None, Some(s)) =>
            validateSoleTraderIdentifiers(s).map { soleTraderIdentifiers =>
              LegalEntityDetails(
                customerIdentification1 = soleTraderIdentifiers._1,
                customerIdentification2 = soleTraderIdentifiers._2,
                organisationName = None,
                firstName = Some(s.fullName.firstName),
                lastName = Some(s.fullName.lastName),
                customerType = CustomerType.Individual,
                registrationDate = registrationDate,
                liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear,
                _,
                _
              )
            }
          case _                     => DataValidationError(DataMissing, missingErrorMessage("Sole trader data")).invalidNel
        }
      case _                                                                                   =>
        DataValidationError(DataMissing, missingErrorMessage("Entity type")).invalidNel
    }

  }

  private def validateSoleTraderIdentifiers(
    s: SoleTraderEntityJourneyData
  ): ValidationResult[(String, Option[String])] =
    (s.sautr, s.nino) match {
      case (Some(sautr), Some(nino)) => (sautr, Some(nino)).validNel
      case (Some(sautr), _)          => (sautr, None).validNel
      case (_, Some(nino))           => (nino, None).validNel
      case _                         => DataValidationError(DataMissing, missingErrorMessage("Sole trader SA UTR or NINO")).invalidNel
    }

  private def validateAmlRegulatedActivity(registration: Registration): ValidationResult[Registration] =
    registration.carriedOutAmlRegulatedActivityInCurrentFy match {
      case Some(_) => registration.validNel
      case _       =>
        DataValidationError(DataMissing, missingErrorMessage("Carried out AML regulated activity choice")).invalidNel
    }

  private def validateAmlSupervisor(registration: Registration): ValidationResult[String] =
    registration.amlSupervisor match {
      case Some(AmlSupervisor(GamblingCommission | FinancialConductAuthority, _)) =>
        DataValidationError(DataInvalid, "AML supervisor cannot be GC or FCA").invalidNel
      case Some(AmlSupervisor(Hmrc, _))                                           => Hmrc.toString.validNel
      case Some(AmlSupervisor(_, Some(otherProfessionalBody)))                    => otherProfessionalBody.validNel
      case _                                                                      =>
        DataValidationError(DataMissing, missingErrorMessage("AML supervisor")).invalidNel
    }

  private def validateRevenueMeetsThreshold(registration: Registration): ValidationResult[Registration] =
    registration.revenueMeetsThreshold match {
      case Some(_) => registration.validNel
      case _       => DataValidationError(DataMissing, missingErrorMessage("Revenue meets threshold flag")).invalidNel
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
            ).validNel
          case _                   => DataValidationError(DataInvalid, "Contact address has no address lines").invalidNel
        }
      case _             => DataValidationError(DataMissing, missingErrorMessage("Contact address")).invalidNel
    }

  private def validateOptExists[T](optData: Option[T], description: String): ValidationResult[T] =
    optData match {
      case Some(value) => value.validNel
      case _           => DataValidationError(DataMissing, missingErrorMessage(description)).invalidNel
    }

  private def validateConditionalOptExists[T](
    optData: Option[T],
    condition: Boolean,
    description: String
  ): ValidationResult[Option[T]] =
    if (condition) {
      optData match {
        case Some(value) => Some(value).validNel
        case _           => DataValidationError(DataMissing, missingErrorMessage(description)).invalidNel
      }
    } else {
      None.validNel
    }

  private def missingErrorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

  private def validateOtherEntity(
    registration: Registration
  ): ValidationResult[Either[EclSubscription, Registration]] =
    (
      validateAmlRegulatedActivityCommonFields(registration),
      validateOptExists(registration.businessSector, "Business sector"),
      validateContactDetails("First", registration.contacts.firstContactDetails),
      validateSecondContactDetails(registration.contacts),
      validateEclAddress(registration.contactAddress),
      validateOptExists(registration.optOtherEntityJourneyData, "Other entity data"),
      validateOptExists(registration.otherEntityJourneyData.businessName, "Business name"),
      registration.entityType match {
        case None                            => DataValidationError(DataMissing, missingErrorMessage("Other entity type")).invalidNel
        case Some(Charity)                   => validateCharity(registration)
        case Some(UnincorporatedAssociation) => validateUnincorporatedAssociation(registration)
        case Some(Trust)                     => validateTrust(registration)
        case Some(NonUKEstablishment)        => validateNonUkEstablishment(registration)
      }
    ).mapN {
      (
        _,
        _,
        _,
        _,
        _,
        _,
        _,
        _
      ) =>
        Right(registration)
    }

  private def validateAmlRegulatedActivityCommonFields(
    registration: Registration
  ): ValidationResult[Either[EclSubscription, Registration]] =
    if (registration.carriedOutAmlRegulatedActivityInCurrentFy.contains(true)) {
      (
        validateAmlSupervisor(registration),
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
          _,
          _,
          _,
          _,
          _
        ) =>
          Right(registration)
      }
    } else { validateAmlRegulatedActivity(registration).map(_ => Right(registration)) }

  private def validateCharity(
    registration: Registration
  ): ValidationResult[Either[EclSubscription, Registration]] = {
    val otherEntityJourneyData = registration.otherEntityJourneyData
    (
      validateOptExists(otherEntityJourneyData.charityRegistrationNumber, "Charity registration number"),
      validateOptExists(otherEntityJourneyData.companyRegistrationNumber, "Company registration number")
    ).mapN {
      (
        _,
        _
      ) =>
        Right(registration)
    }
  }

  private def validateUnincorporatedAssociation(
    registration: Registration
  ): ValidationResult[Either[EclSubscription, Registration]] = {
    val otherEntityJourneyData = registration.otherEntityJourneyData
    (
      validateOptExists(otherEntityJourneyData.isCtUtrPresent, "Corporation Tax Unique Taxpayer Reference choice"),
      validateConditionalOptExists(
        otherEntityJourneyData.ctUtr,
        otherEntityJourneyData.isCtUtrPresent.contains(true),
        "Corporation Tax Unique Taxpayer Reference"
      )
    ).mapN((_, _) => Right(registration))
  }

  private def validateTrust(registration: Registration): ValidationResult[Either[EclSubscription, Registration]] =
    validateOptExists(registration.otherEntityJourneyData.ctUtr, "Corporation Tax Unique Taxpayer Reference")
      .map(_ => Right(registration))

  private def validateNonUkEstablishment(
    registration: Registration
  ): ValidationResult[Either[EclSubscription, Registration]] = {
    val data = registration.otherEntityJourneyData
    (
      validateOptExists(data.hasUkCrn, "Has uk crn"),
      validateConditionalOptExists(
        data.companyRegistrationNumber,
        data.hasUkCrn.contains(true),
        "Company registration number"
      ),
      validateOptExists(data.utrType, "Utr type"),
      validateConditionalOptExists(
        data.ctUtr,
        data.utrType.contains(CtUtr),
        "Corporation Tax Unique Taxpayer Reference"
      ),
      validateConditionalOptExists(
        data.saUtr,
        data.utrType.contains(SaUtr),
        "Self Assessment Unique Taxpayer Reference"
      )
    ).mapN {
      (
        _,
        _,
        _,
        _,
        _
      ) =>
        Right(registration)
    }
  }
}
