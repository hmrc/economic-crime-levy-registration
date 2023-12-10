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

import org.mockito.ArgumentMatchers.any
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyregistration._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Other
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.IncorporatedEntityJourneyData
import uk.gov.hmrc.economiccrimelevyregistration.utils.SchemaValidator

import java.time.{Clock, Instant, ZoneId}

class RegistrationValidationServiceSpec extends SpecBase {

  private val fixedPointInTime = Instant.parse("2007-12-25T10:15:30.00Z")
  private val stubClock: Clock = Clock.fixed(fixedPointInTime, ZoneId.systemDefault)

  val mockSchemaValidator: SchemaValidator = mock[SchemaValidator]

  val service = new RegistrationValidationService(stubClock, mockSchemaValidator)

  "validateRegistration" should {
    "return the ECL subscription if the registration for an incorporated entity is valid" in forAll {
      (
        validIncorporatedEntityRegistration: ValidIncorporatedEntityRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val expectedEclSubscription        = validIncorporatedEntityRegistration.expectedEclSubscription
        val updatedExpectedEclSubscription = expectedEclSubscription.copy(subscription =
          expectedEclSubscription.subscription.copy(legalEntityDetails =
            expectedEclSubscription.subscription.legalEntityDetails
              .copy(liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear)
          )
        )

        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            any(),
            any()
          )(any())
        ).thenReturn(Right(()))

        val result =
          service.validateSubscription(validIncorporatedEntityRegistration.registration, registrationAdditionalInfo)

        result shouldBe Right(updatedExpectedEclSubscription)
    }

    "return the ECL subscription if the registration for a sole trader is valid" in forAll {
      (
        validSoleTraderRegistration: ValidSoleTraderRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val expectedEclSubscription        = validSoleTraderRegistration.expectedEclSubscription
        val updatedExpectedEclSubscription = expectedEclSubscription.copy(subscription =
          expectedEclSubscription.subscription.copy(legalEntityDetails =
            expectedEclSubscription.subscription.legalEntityDetails
              .copy(liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear)
          )
        )

        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            any(),
            any()
          )(any())
        ).thenReturn(Right(()))

        val result = service.validateSubscription(validSoleTraderRegistration.registration, registrationAdditionalInfo)
        result shouldBe Right(updatedExpectedEclSubscription)
    }

    "return the ECL subscription if the registration for a limited partnership is valid" in forAll {
      (
        validLimitedPartnershipRegistration: ValidLimitedPartnershipRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val expectedEclSubscription        = validLimitedPartnershipRegistration.expectedEclSubscription
        val updatedExpectedEclSubscription = expectedEclSubscription.copy(subscription =
          expectedEclSubscription.subscription.copy(legalEntityDetails =
            expectedEclSubscription.subscription.legalEntityDetails
              .copy(liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear)
          )
        )

        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            any(),
            any()
          )(any())
        ).thenReturn(Right(()))

        val result =
          service.validateSubscription(validLimitedPartnershipRegistration.registration, registrationAdditionalInfo)
        result shouldBe Right(updatedExpectedEclSubscription)
    }

    "return the ECL subscription if the registration for a scottish or general partnership is valid" in forAll {
      (
        validScottishOrGeneralPartnershipRegistration: ValidScottishOrGeneralPartnershipRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val expectedEclSubscription        = validScottishOrGeneralPartnershipRegistration.expectedEclSubscription
        val updatedExpectedEclSubscription = expectedEclSubscription.copy(subscription =
          expectedEclSubscription.subscription.copy(legalEntityDetails =
            expectedEclSubscription.subscription.legalEntityDetails
              .copy(liabilityStartDate = registrationAdditionalInfo.StartOfEclFinancialYear)
          )
        )

        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            any(),
            any()
          )(any())
        ).thenReturn(Right(()))

        val result = service.validateSubscription(
          validScottishOrGeneralPartnershipRegistration.registration,
          registrationAdditionalInfo
        )

        result shouldBe Right(updatedExpectedEclSubscription)
    }

    "return a non-empty list of errors when unconditional mandatory registration data items are missing" in {
      val registration = Registration
        .empty("internalId")
        .copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(true))

      val result = service.validateRegistration(registration)

      result shouldBe Left(DataValidationError.DataInvalid("Entity type missing"))
    }

    "return errors if the entity type is an incorporated entity type but there is no incorporated entity data in the registration" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validRegistration.registration.copy(incorporatedEntityJourneyData = None)

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Incorporated entity data is missing"))
    }

    "return an error if the relevant AP is not 12 months and the relevant AP length is missing" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration =
          validRegistration.registration.copy(relevantAp12Months = Some(false), relevantApLength = None)

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Relevant AP length is missing"))

    }

    "return errors if the entity type is partnership but there is no partnership data in the registration" in forAll {
      (
        validLimitedPartnershipRegistration: ValidLimitedPartnershipRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validLimitedPartnershipRegistration.registration
          .copy(partnershipEntityJourneyData = None)

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Partnership data is missing"))

    }

    "return errors if the entity type is sole trader but there is no sole trader data in the registration" in forAll {
      (
        validSoleTraderRegistration: ValidSoleTraderRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validSoleTraderRegistration.registration
          .copy(soleTraderEntityJourneyData = None)

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Sole trader data is missing"))
    }

    "return an error if the registration data does not contain the business partner ID" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validRegistration.registration
          .copy(incorporatedEntityJourneyData =
            Some(
              incorporatedEntityJourneyData.copy(registration =
                incorporatedEntityJourneyData.registration.copy(registeredBusinessPartnerId = None)
              )
            )
          )

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Business partner ID is missing"))
    }

    "return errors if the second contact choice is true and there are no second contact details in the registration data" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val validContacts       = validRegistration.registration.contacts
        val invalidRegistration = validRegistration.registration.copy(contacts =
          validContacts.copy(secondContact = Some(true), secondContactDetails = ContactDetails(None, None, None, None))
        )

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Second contact name is missing"))
    }

    "return an error if the registration data does not contain the AML regulated activity choice" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validRegistration.registration
          .copy(carriedOutAmlRegulatedActivityInCurrentFy = None)

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Carried out AML regulated activity choice is missing"))

    }

    "return an error if the registration data contains GC or FCA as the AML supervisor" in forAll(
      Arbitrary.arbitrary[ValidIncorporatedEntityRegistration],
      Gen.oneOf(GamblingCommission, FinancialConductAuthority),
      Arbitrary.arbitrary[RegistrationAdditionalInfo]
    ) {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        gcOrFca: AmlSupervisorType,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validRegistration.registration
          .copy(amlSupervisor = Some(AmlSupervisor(gcOrFca, None)))

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataInvalid("AML supervisor cannot be GC or FCA"))
    }

    "return an error if the registration data contains the revenue meets threshold flag not set" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validRegistration.registration
          .copy(revenueMeetsThreshold = None)

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Revenue meets threshold flag is missing"))
    }

    "return an error if the contact address contains no address lines" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidRegistration = validRegistration.registration
          .copy(contactAddress =
            validRegistration.registration.contactAddress.map(_.copy(None, None, None, None, None, None, None, None))
          )

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Contact address has no address lines"))
    }

    "return errors if the registration data contains no partnership name, SA UTR and " +
      "postcode when the entity type is general or scottish partnership" in forAll {
        (
          validScottishOrGeneralPartnershipRegistration: ValidScottishOrGeneralPartnershipRegistration,
          registrationAdditionalInfo: RegistrationAdditionalInfo
        ) =>
          val invalidPartnershipData =
            validScottishOrGeneralPartnershipRegistration.registration.partnershipEntityJourneyData.map(
              _.copy(
                sautr = None,
                postcode = None
              )
            )

          val invalidRegistration = validScottishOrGeneralPartnershipRegistration.registration
            .copy(partnershipEntityJourneyData = invalidPartnershipData, partnershipName = None)

          val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

          result shouldBe Left(DataValidationError.DataMissing("Partnership SA UTR is missing"))
      }

    "return errors if the registration data contains no partnership SA UTR and " +
      "company profile when the entity type is limited, limited liability or scottish limited partnership" in forAll {
        (
          validLimitedPartnershipRegistration: ValidLimitedPartnershipRegistration,
          registrationAdditionalInfo: RegistrationAdditionalInfo
        ) =>
          val invalidPartnershipData =
            validLimitedPartnershipRegistration.registration.partnershipEntityJourneyData.map(
              _.copy(
                sautr = None,
                companyProfile = None
              )
            )

          val invalidRegistration = validLimitedPartnershipRegistration.registration
            .copy(partnershipEntityJourneyData = invalidPartnershipData)

          val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

          result shouldBe Left(DataValidationError.DataMissing("Partnership SA UTR is missing"))
      }

    "return errors if the registration data contains no sole trader SA UTR or NINO and the entity type is sole trader" in forAll {
      (
        validSoleTraderRegistration: ValidSoleTraderRegistration,
        registrationAdditionalInfo: RegistrationAdditionalInfo
      ) =>
        val invalidSoleTraderData = validSoleTraderRegistration.registration.soleTraderEntityJourneyData.map(
          _.copy(
            sautr = None,
            nino = None
          )
        )

        val invalidRegistration =
          validSoleTraderRegistration.registration.copy(soleTraderEntityJourneyData = invalidSoleTraderData)

        val result = service.validateSubscription(invalidRegistration, registrationAdditionalInfo)

        result shouldBe Left(DataValidationError.DataMissing("Sole trader SA UTR or NINO is missing"))

    }

    "return errors if the registration for a other entity is invalid" in {
      val invalidRegistration = Registration
        .empty("")
        .copy(
          entityType = Some(Other),
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true)
        )
      val result              = service.validateRegistration(invalidRegistration)

      result shouldBe Left(DataValidationError.DataMissing("AML supervisor is missing"))
    }

    "return the registration if the registration for a charity is valid" in forAll {
      (validCharityRegistration: ValidCharityRegistration) =>
        val result = service.validateRegistration(validCharityRegistration.registration)
        result shouldBe Right(validCharityRegistration.registration)
    }

    "return errors if the registration for a charity is invalid" in forAll {
      (validCharityRegistration: ValidCharityRegistration) =>
        val otherEntityJourneyData     = validCharityRegistration.registration.otherEntityJourneyData.copy(
          charityRegistrationNumber = None,
          companyRegistrationNumber = None
        )
        val invalidCharityRegistration = validCharityRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        val result = service.validateRegistration(invalidCharityRegistration)

        result shouldBe Left(DataValidationError.DataMissing("Charity registration number is missing"))
    }

    "return errors if the registration for a unincorporated association is invalid when isCtUtrPresent flag is not present" in {
      (
        unincorporatedAssociationRegistration: ValidUnincorporatedAssociationRegistration
      ) =>
        val otherEntityJourneyData                       = unincorporatedAssociationRegistration.registration.otherEntityJourneyData.copy(
          isCtUtrPresent = None,
          ctUtr = None
        )
        val invalidUnincorporatedAssociationRegistration = unincorporatedAssociationRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )
        val result                                       =
          service.validateRegistration(invalidUnincorporatedAssociationRegistration)

        result shouldBe Left(
          DataValidationError.DataMissing("Corporation Tax Unique Taxpayer Reference choice is missing")
        )
    }

    "return errors if the registration for a unincorporated association is invalid when CT-UTR is not present" in {
      (
        unincorporatedAssociationRegistration: ValidUnincorporatedAssociationRegistration
      ) =>
        val otherEntityJourneyData                       = unincorporatedAssociationRegistration.registration.otherEntityJourneyData.copy(
          isCtUtrPresent = Some(true),
          ctUtr = None
        )
        val invalidUnincorporatedAssociationRegistration = unincorporatedAssociationRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        val result =
          service.validateRegistration(invalidUnincorporatedAssociationRegistration)

        result shouldBe Left(DataValidationError.DataMissing("Corporation Tax Unique Taxpayer Reference is missing"))
    }

    "return the registration if the registration for an unincorporated association is valid" in {
      (
        unincorporatedAssociationRegistration: ValidUnincorporatedAssociationRegistration
      ) =>
        val otherEntityJourneyData = unincorporatedAssociationRegistration.registration.otherEntityJourneyData.copy(
          isCtUtrPresent = Some(true)
        )
        val validRegistration      = unincorporatedAssociationRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        val result = service.validateRegistration(validRegistration)

        result shouldBe Right(validRegistration)
    }

    "return errors if the registration for a Trust is invalid when CT-UTR is not present" in {
      (trustRegistration: ValidTrustRegistration) =>
        val otherEntityJourneyData   = trustRegistration.registration.otherEntityJourneyData.copy(
          ctUtr = None
        )
        val invalidTrustRegistration = trustRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        val result = service.validateRegistration(invalidTrustRegistration)
        result shouldBe Left(DataValidationError.DataMissing("Corporation Tax Unique Taxpayer Reference is missing"))
    }

    "return the registration if the registration for a Trust is valid" in {
      (trustRegistration: ValidTrustRegistration) =>
        val result = service.validateRegistration(trustRegistration.registration)
        result shouldBe Right(trustRegistration.registration)
    }

    "return the registration if the registration for a non-Uk establishment is valid" in forAll {
      (
        validNonUkEstablishmentRegistration: ValidNonUkEstablishmentRegistration
      ) =>
        val result =
          service.validateRegistration(validNonUkEstablishmentRegistration.registration)
        result shouldBe Right(validNonUkEstablishmentRegistration.registration)
    }

    "return errors if the registration for a non-UK establishment is invalid" in forAll {
      (
        validNonUkEstablishmentRegistration: ValidNonUkEstablishmentRegistration,
        none: Boolean,
        utrType: UtrType
      ) =>
        val otherEntityJourneyData     = validNonUkEstablishmentRegistration.registration.otherEntityJourneyData.copy(
          companyRegistrationNumber = None,
          utrType = if (none) None else Some(utrType),
          ctUtr = None,
          saUtr = None
        )
        val invalidCharityRegistration = validNonUkEstablishmentRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        val result = service.validateRegistration(invalidCharityRegistration)
        result shouldBe Left(DataValidationError.DataMissing("Company registration number is missing"))
    }
  }
}
