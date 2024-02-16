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

import cats.data.EitherT
import cats.implicits._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.UtrType.{CtUtr, SaUtr}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.LegalEntityDetails.CustomerType
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyregistration.utils.StringUtils._
import uk.gov.hmrc.economiccrimelevyregistration.utils.{SchemaLoader, SchemaValidator}

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.Future

class RegistrationValidationService @Inject() (clock: Clock, schemaValidator: SchemaValidator) {

  private type ValidationResult[T] = Either[DataValidationError, T]

  def validateRegistration(
    eclRegistrationModel: EclRegistrationModel
  ): EitherT[Future, DataValidationError, Registration] =
    EitherT {
      val registration = eclRegistrationModel.registration
      Future.successful(
        registration.entityType match {
          case Some(value) if EntityType.isOther(value) => validateOtherEntity(registration)
          case _                                        =>
            (registration.registrationType, registration.entityType) match {
              case (Some(Amendment), _)  => transformToAmendedEclRegistration(registration)
              case (Some(Initial), None) => Left(DataValidationError.DataMissing("Entity type missing"))
              case (_, _)                => Left(DataValidationError.DataInvalid("Wrong registrationType is passed"))
            }
        }
      )
    }

  def validateSubscription(
    eclRegistrationModel: EclRegistrationModel
  ): EitherT[Future, DataValidationError, EclSubscription] =
    EitherT {
      Future.successful(
        transformToEclSubscription(eclRegistrationModel.registration, eclRegistrationModel.additionalInfo) match {
          case Right(eclSubscription) =>
            schemaValidator.validateAgainstJsonSchema(
              eclSubscription.subscription,
              SchemaLoader.loadSchema("create-ecl-subscription-request.json")
            ) match {
              case Right(_)    => Right(eclSubscription)
              case Left(error) => Left(error)
            }
          case Left(error)            => Left(error)
        }
      )
    }

  private def transformToEclSubscription(
    registration: Registration,
    registrationAdditionalInfo: RegistrationAdditionalInfo
  ): ValidationResult[EclSubscription] =
    for {
      _                    <- validateAmlRegulatedActivityCommonFields(registration)
      legalEntityDetails   <- validateLegalEntityDetails(registration, registrationAdditionalInfo)
      businessPartnerId    <- validateBusinessPartnerId(registration)
      businessSector       <- validateOptExists(registration.businessSector, "Business sector")
      firstContactDetails  <- validateContactDetails("First", registration.contacts.firstContactDetails)
      secondContactDetails <- validateSecondContactDetails(registration.contacts)
      contactAddress       <- validateEclAddress(registration.contactAddress)
    } yield EclSubscription(
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

  private def transformToAmendedEclRegistration(
    registration: Registration
  ): ValidationResult[Registration] =
    for {
      _ <- validateAmlSupervisor(registration)
      _ <- validateOptExists(registration.businessSector, "Business sector")
      _ <- validateContactDetails("First", registration.contacts.firstContactDetails)
      _ <- validateSecondContactDetails(registration.contacts)
      _ <- validateEclAddress(registration.contactAddress)
      _ <- validateAmendmentReason(registration)
    } yield registration

  private def validateContactDetails(
    firstOrSecond: String,
    details: ContactDetails
  ): ValidationResult[SubscriptionContactDetails] =
    for {
      name   <- validateOptExists(details.name, s"$firstOrSecond contact name")
      role   <- validateOptExists(details.role, s"$firstOrSecond contact role")
      email  <- validateOptExists(details.emailAddress, s"$firstOrSecond contact email")
      number <- validateOptExists(details.telephoneNumber, s"$firstOrSecond contact number")
    } yield SubscriptionContactDetails(
      name = name,
      positionInCompany = role,
      telephone = number.removeWhitespace,
      emailAddress = email
    )

  private def validateSecondContactDetails(
    contacts: Contacts
  ): ValidationResult[Option[SubscriptionContactDetails]] =
    contacts.secondContact match {
      case Some(true)  => validateContactDetails("Second", contacts.secondContactDetails).map(Some(_))
      case Some(false) => Right(None)
      case _           => Left(DataValidationError.DataMissing(missingErrorMessage("Second contact choice")))
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
            Right(
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
            )
          case _                     => Left(DataValidationError.DataMissing(missingErrorMessage("Incorporated entity data")))
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
          case _                      => Left(DataValidationError.DataMissing(missingErrorMessage("Partnership data")))
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
          case _                     => Left(DataValidationError.DataMissing(missingErrorMessage("Partnership data")))
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
          case _                     => Left(DataValidationError.DataMissing(missingErrorMessage("Sole trader data")))
        }
      case _                                                                                   =>
        Left(DataValidationError.DataMissing(missingErrorMessage("Entity type")))
    }

  }

  private def validateSoleTraderIdentifiers(
    s: SoleTraderEntityJourneyData
  ): ValidationResult[(String, Option[String])] =
    (s.sautr, s.nino) match {
      case (Some(sautr), Some(nino)) => Right((sautr, Some(nino)))
      case (Some(sautr), _)          => Right((sautr, None))
      case (_, Some(nino))           => Right((nino, None))
      case _                         => Left(DataValidationError.DataMissing(missingErrorMessage("Sole trader SA UTR or NINO")))
    }

  private def validateAmlRegulatedActivity(registration: Registration): ValidationResult[Registration] =
    registration.carriedOutAmlRegulatedActivityInCurrentFy match {
      case Some(_) => Right(registration)
      case _       =>
        Left(DataValidationError.DataMissing(missingErrorMessage("Carried out AML regulated activity choice")))
    }

  private def validateAmlSupervisor(registration: Registration): ValidationResult[Unit] =
    registration.amlSupervisor match {
      case Some(AmlSupervisor(GamblingCommission | FinancialConductAuthority, _)) =>
        Left(DataValidationError.DataInvalid("AML supervisor cannot be GC or FCA"))
      case Some(AmlSupervisor(Hmrc, _))                                           => Right(())
      case Some(AmlSupervisor(_, Some(_)))                                        => Right(())
      case _                                                                      =>
        Left(DataValidationError.DataMissing(missingErrorMessage("AML supervisor")))
    }

  private def validateRevenueMeetsThreshold(registration: Registration): ValidationResult[Registration] =
    registration.revenueMeetsThreshold match {
      case Some(_) => Right(registration)
      case _       => Left(DataValidationError.DataMissing(missingErrorMessage("Revenue meets threshold flag")))
    }

  private def validateAmendmentReason(registration: Registration): ValidationResult[Registration] =
    registration.amendReason match {
      case Some(_) => Right(registration)
      case _       => Left(DataValidationError.DataMissing(missingErrorMessage("Reason for amendment")))
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
            Right(
              CorrespondenceAddressDetails(
                line1,
                otherLines,
                address.postCode,
                address.countryCode
              )
            )
          case _                   => Left(DataValidationError.DataMissing("Contact address has no address lines"))
        }
      case _             => Left(DataValidationError.DataMissing(missingErrorMessage("Contact address")))
    }

  private def validateOptExists[T](optData: Option[T], description: String): ValidationResult[T] =
    optData match {
      case Some(value) => Right(value)
      case _           => Left(DataValidationError.DataMissing(missingErrorMessage(description)))
    }

  private def validateConditionalOptExists[T](
    optData: Option[T],
    condition: Boolean,
    description: String
  ): ValidationResult[Option[T]] =
    if (condition) {
      optData match {
        case Some(value) => Right(Some(value))
        case _           => Left(DataValidationError.DataMissing(missingErrorMessage(description)))
      }
    } else {
      Right(None)
    }

  private def missingErrorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

  private def validateOtherEntity(
    registration: Registration
  ): ValidationResult[Registration] =
    for {
      _ <- validateAmlRegulatedActivityCommonFields(registration)
      _ <- validateOptExists(registration.businessSector, "Business sector")
      _ <- validateContactDetails("First", registration.contacts.firstContactDetails)
      _ <- validateSecondContactDetails(registration.contacts)
      _ <- validateEclAddress(registration.contactAddress)
      _ <- validateOptExists(registration.optOtherEntityJourneyData, "Other entity data")
      _ <- validateOptExists(registration.otherEntityJourneyData.businessName, "Business name")
      _ <- registration.entityType match {
             case None                            => Left(DataValidationError.DataMissing(missingErrorMessage("Other entity type")))
             case Some(Charity)                   => validateCharity(registration)
             case Some(UnincorporatedAssociation) => validateUnincorporatedAssociation(registration)
             case Some(Trust)                     => validateTrust(registration)
             case Some(NonUKEstablishment)        => validateNonUkEstablishment(registration)
           }
    } yield registration

  private def validateAmlRegulatedActivityCommonFields(
    registration: Registration
  ): ValidationResult[Registration] =
    if (registration.carriedOutAmlRegulatedActivityInCurrentFy.contains(true)) {
      for {
        _ <- validateAmlSupervisor(registration)
        _ <- validateOptExists(registration.relevantAp12Months, "Relevant AP 12 months choice")
        _ <- validateOptExists(registration.relevantApRevenue, "Relevant AP revenue")
        _ <- validateConditionalOptExists(
               registration.relevantApLength,
               registration.relevantAp12Months.contains(false),
               "Relevant AP length"
             )
        _ <- validateRevenueMeetsThreshold(registration)
      } yield registration
    } else { validateAmlRegulatedActivity(registration).map(_ => registration) }

  private def validateCharity(
    registration: Registration
  ): ValidationResult[Unit] = {
    val otherEntityJourneyData = registration.otherEntityJourneyData

    for {
      _ <- validateOptExists(otherEntityJourneyData.charityRegistrationNumber, "Charity registration number")
      _ <- validateOptExists(otherEntityJourneyData.companyRegistrationNumber, "Company registration number")
      _ <- validateOptExists(otherEntityJourneyData.isCtUtrPresent, "Corporation Tax Unique Taxpayer Reference choice")
      _ <- validateConditionalOptExists(
             otherEntityJourneyData.ctUtr,
             otherEntityJourneyData.isCtUtrPresent.contains(true),
             "Unique Taxpayer Reference"
           )
    } yield Right(())
  }

  private def validateUnincorporatedAssociation(
    registration: Registration
  ): ValidationResult[Unit] = {
    val data = registration.otherEntityJourneyData

    for {
      _ <- validateOptExists(data.isUkCrnPresent, "Has uk crn")
      _ <- validateConditionalOptExists(
             data.companyRegistrationNumber,
             data.isUkCrnPresent.contains(true),
             "Company registration number"
           )
      _ <- validateOptExists(data.utrType, "Utr type")
      _ <- validateConditionalOptExists(
             data.ctUtr,
             data.utrType.contains(CtUtr),
             "Corporation Tax Unique Taxpayer Reference"
           )
      _ <- validateConditionalOptExists(
             data.saUtr,
             data.utrType.contains(SaUtr),
             "Self Assessment Unique Taxpayer Reference"
           )
    } yield Right(())
  }

  private def validateTrust(registration: Registration): ValidationResult[Unit] =
    validateOptExists(registration.otherEntityJourneyData.ctUtr, "Corporation Tax Unique Taxpayer Reference")
      .map(_ => Right(()))

  private def validateNonUkEstablishment(
    registration: Registration
  ): ValidationResult[Unit] = {
    val data = registration.otherEntityJourneyData
    for {
      _ <- validateOptExists(data.isUkCrnPresent, "Has uk crn")
      _ <- validateConditionalOptExists(
             data.companyRegistrationNumber,
             data.isUkCrnPresent.contains(true),
             "Company registration number"
           )
      _ <- validateOptExists(data.utrType, "Utr type")
      _ <- validateConditionalOptExists(
             data.ctUtr,
             data.utrType.contains(CtUtr),
             "Corporation Tax Unique Taxpayer Reference"
           )
      _ <- validateConditionalOptExists(
             data.saUtr,
             data.utrType.contains(SaUtr),
             "Self Assessment Unique Taxpayer Reference"
           )
    } yield Right(())
  }
}
