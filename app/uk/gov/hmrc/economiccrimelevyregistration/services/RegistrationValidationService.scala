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

import cats.data.Validated.{Valid, invalid}
import cats.data.ValidatedNel
import cats.implicits._
import play.api.data.validation.Invalid
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.OtherEntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.LegalEntityDetails.{CustomerType, StartOfFirstEclFinancialYear}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyregistration.utils.StringUtils._
import uk.gov.hmrc.economiccrimelevyregistration.utils.{SchemaLoader, SchemaValidator}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneOffset}
import javax.inject.Inject

class RegistrationValidationService @Inject() (clock: Clock, schemaValidator: SchemaValidator) {

  type ValidationResult[A] = ValidatedNel[DataValidationError, A]

  def validateRegistration(registration: Registration): ValidationResult[EclSubscription] =
    registration.entityType match {
      case Some(Other) =>
        validateOtherEntity(registration)
      case _           =>
        transformToEclSubscription(registration) match {
          case Valid(eclSubscription) =>
            schemaValidator
              .validateAgainstJsonSchema(
                eclSubscription.subscription,
                SchemaLoader.loadSchema("create-ecl-subscription-request.json")
              )
              .map(_ => eclSubscription)
          case invalid                => invalid
        }
    }

  private def transformToEclSubscription(registration: Registration): ValidationResult[EclSubscription] =
    (
      validateLegalEntityDetails(registration),
      validateBusinessPartnerId(registration),
      validateAmlSupervisor(registration),
      validateOptExists(registration.businessSector, "Business sector"),
      validateContactDetails("First", registration.contacts.firstContactDetails),
      validateSecondContactDetails(registration.contacts),
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
        businessPartnerId,
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
        EclSubscription(
          businessPartnerId = businessPartnerId,
          subscription = Subscription(
            legalEntityDetails = legalEntityDetails(amlSupervisor, businessSector.toString),
            correspondenceAddressDetails = contactAddress,
            primaryContactDetails = firstContactDetails,
            secondaryContactDetails = secondContactDetails
          ),
          registration = None
        )
    }

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
      DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(Instant.now(clock))

    registration.entityType match {
      case Some(UkLimitedCompany | UnlimitedCompany)                                           =>
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
                liabilityStartDate = StartOfFirstEclFinancialYear,
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
                liabilityStartDate = StartOfFirstEclFinancialYear,
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
                liabilityStartDate = StartOfFirstEclFinancialYear,
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
                liabilityStartDate = StartOfFirstEclFinancialYear,
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
      case Some(true)  => registration.validNel
      case Some(false) =>
        DataValidationError(DataInvalid, "Carried out AML regulated activity cannot be false").invalidNel
      case _           =>
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
      case Some(true)  => registration.validNel
      case Some(false) => DataValidationError(DataInvalid, "Revenue does not meet the liability threshold").invalidNel
      case _           => DataValidationError(DataMissing, missingErrorMessage("Revenue meets threshold flag")).invalidNel
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

  private def validateOtherEntity(registration: Registration): ValidationResult[EclSubscription] =
    validateCommonData(registration) match {
      case Left(registration) =>
        registration.optOtherEntityJourneyData match {
          case Some(journey) =>
            journey.businessName match {
              case Some(value) if !value.isBlank =>
                journey.entityType match {
                  case Some(Charity) => validateCharity(registration, journey)
                  case _             => ???
                }
              case _                             => DataValidationError(DataMissing, missingErrorMessage("business name")).invalidNel
            }
          case _             => DataValidationError(DataMissing, missingErrorMessage("other entity data")).invalidNel
        }
      case Right(error)       => error
    }

  private def validRegistration(registration: Registration): ValidationResult[EclSubscription] =
    Valid(EclSubscription(registration.internalId, Subscription.empty(), Some(registration)))

  private def validateCommonData(registration: Registration): Either[Registration, ValidationResult[EclSubscription]] =
    List(
      registration.amlSupervisor,
      registration.relevantAp12Months,
      registration.relevantApRevenue
    ).exists(!_.nonEmpty) match {
      case true  => Right(DataValidationError(DataMissing, missingErrorMessage("common attributes")).invalidNel)
      case false => Left(registration)
    }

  private def validateCharity(
    registration: Registration,
    journey: OtherEntityJourneyData
  ): ValidationResult[EclSubscription] =
    journey.charityRegistrationNumber match {
      case Some(value) if !value.isBlank =>
        journey.companyRegistrationNumber match {
          case Some(value) if !value.isBlank => validRegistration(registration)
          case _                             => DataValidationError(DataMissing, missingErrorMessage("company registration number")).invalidNel
        }
      case _                             => DataValidationError(DataMissing, missingErrorMessage("charity registration number")).invalidNel
    }
}
