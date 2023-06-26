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
import cats.implicits.catsSyntaxValidatedId
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyregistration._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Other
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.IncorporatedEntityJourneyData
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, AmlSupervisorType, ContactDetails, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.utils.SchemaValidator

import java.time.{Clock, Instant, ZoneId}

class RegistrationValidationServiceSpec extends SpecBase {

  private val fixedPointInTime = Instant.parse("2007-12-25T10:15:30.00Z")
  private val stubClock: Clock = Clock.fixed(fixedPointInTime, ZoneId.systemDefault)

  val mockSchemaValidator: SchemaValidator = mock[SchemaValidator]

  val service = new RegistrationValidationService(stubClock, mockSchemaValidator)

  "validateRegistration" should {
    "return the ECL subscription if the registration for an incorporated entity is valid" in forAll {
      validIncorporatedEntityRegistration: ValidIncorporatedEntityRegistration =>
        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            ArgumentMatchers.eq(validIncorporatedEntityRegistration.expectedEclSubscription.subscription),
            any()
          )(any())
        ).thenReturn(validIncorporatedEntityRegistration.expectedEclSubscription.subscription.validNel)

        val result = service.validateRegistration(validIncorporatedEntityRegistration.registration)
        result shouldBe Valid(Left(validIncorporatedEntityRegistration.expectedEclSubscription))
    }

    "return the ECL subscription if the registration for a sole trader is valid" in forAll {
      validSoleTraderRegistration: ValidSoleTraderRegistration =>
        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            ArgumentMatchers.eq(validSoleTraderRegistration.expectedEclSubscription.subscription),
            any()
          )(any())
        ).thenReturn(validSoleTraderRegistration.expectedEclSubscription.subscription.validNel)

        val result = service.validateRegistration(validSoleTraderRegistration.registration)
        result shouldBe Valid(Left(validSoleTraderRegistration.expectedEclSubscription))
    }

    "return the ECL subscription if the registration for a limited partnership is valid" in forAll {
      validLimitedPartnershipRegistration: ValidLimitedPartnershipRegistration =>
        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            ArgumentMatchers.eq(validLimitedPartnershipRegistration.expectedEclSubscription.subscription),
            any()
          )(any())
        ).thenReturn(validLimitedPartnershipRegistration.expectedEclSubscription.subscription.validNel)

        val result = service.validateRegistration(validLimitedPartnershipRegistration.registration)
        result shouldBe Valid(Left(validLimitedPartnershipRegistration.expectedEclSubscription))
    }

    "return the ECL subscription if the registration for a scottish or general partnership is valid" in forAll {
      validScottishOrGeneralPartnershipRegistration: ValidScottishOrGeneralPartnershipRegistration =>
        when(
          mockSchemaValidator.validateAgainstJsonSchema(
            ArgumentMatchers.eq(validScottishOrGeneralPartnershipRegistration.expectedEclSubscription.subscription),
            any()
          )(any())
        ).thenReturn(validScottishOrGeneralPartnershipRegistration.expectedEclSubscription.subscription.validNel)

        val result = service.validateRegistration(validScottishOrGeneralPartnershipRegistration.registration)
        result shouldBe Valid(Left(validScottishOrGeneralPartnershipRegistration.expectedEclSubscription))
    }

    "return a non-empty list of errors when unconditional mandatory registration data items are missing" in {
      val registration = Registration.empty("internalId")

      val expectedErrors = Seq(
        DataValidationError(DataMissing, "Carried out AML regulated activity choice is missing"),
        DataValidationError(DataMissing, "Business partner ID is missing"),
        DataValidationError(DataMissing, "Relevant AP 12 months choice is missing"),
        DataValidationError(DataMissing, "Relevant AP revenue is missing"),
        DataValidationError(DataMissing, "Revenue meets threshold flag is missing"),
        DataValidationError(DataMissing, "AML supervisor is missing"),
        DataValidationError(DataMissing, "Business sector is missing"),
        DataValidationError(DataMissing, "First contact name is missing"),
        DataValidationError(DataMissing, "First contact role is missing"),
        DataValidationError(DataMissing, "First contact email is missing"),
        DataValidationError(DataMissing, "First contact number is missing"),
        DataValidationError(DataMissing, "Contact address is missing"),
        DataValidationError(DataMissing, "Entity type is missing"),
        DataValidationError(DataMissing, "Second contact choice is missing")
      )

      val result = service.validateRegistration(registration)

      result.isValid shouldBe false
      result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
    }

    "return errors if the entity type is an incorporated entity type but there is no incorporated entity data in the registration" in forAll {
      validRegistration: ValidIncorporatedEntityRegistration =>
        val invalidRegistration = validRegistration.registration.copy(incorporatedEntityJourneyData = None)

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain theSameElementsAs Seq(
            DataValidationError(
              DataMissing,
              "Incorporated entity data is missing"
            ),
            DataValidationError(DataMissing, "Business partner ID is missing")
          )
        )
    }

    "return an error if the relevant AP is not 12 months and the relevant AP length is missing" in forAll {
      validRegistration: ValidIncorporatedEntityRegistration =>
        val invalidRegistration =
          validRegistration.registration.copy(relevantAp12Months = Some(false), relevantApLength = None)

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain only DataValidationError(
            DataMissing,
            "Relevant AP length is missing"
          )
        )
    }

    "return errors if the entity type is partnership but there is no partnership data in the registration" in forAll {
      (validLimitedPartnershipRegistration: ValidLimitedPartnershipRegistration) =>
        val invalidRegistration = validLimitedPartnershipRegistration.registration
          .copy(partnershipEntityJourneyData = None)

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain theSameElementsAs
            Seq(
              DataValidationError(DataMissing, "Partnership data is missing"),
              DataValidationError(DataMissing, "Business partner ID is missing")
            )
        )
    }

    "return errors if the entity type is sole trader but there is no sole trader data in the registration" in forAll {
      (validSoleTraderRegistration: ValidSoleTraderRegistration) =>
        val invalidRegistration = validSoleTraderRegistration.registration
          .copy(soleTraderEntityJourneyData = None)

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain theSameElementsAs Seq(
            DataValidationError(DataMissing, "Sole trader data is missing"),
            DataValidationError(DataMissing, "Business partner ID is missing")
          )
        )
    }

    "return an error if the registration data does not contain the business partner ID" in forAll {
      (
        validRegistration: ValidIncorporatedEntityRegistration,
        incorporatedEntityJourneyData: IncorporatedEntityJourneyData
      ) =>
        val invalidRegistration = validRegistration.registration
          .copy(incorporatedEntityJourneyData =
            Some(
              incorporatedEntityJourneyData.copy(registration =
                incorporatedEntityJourneyData.registration.copy(registeredBusinessPartnerId = None)
              )
            )
          )

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain only DataValidationError(
            DataMissing,
            "Business partner ID is missing"
          )
        )
    }

    "return errors if the second contact choice is true and there are no second contact details in the registration data" in forAll {
      (validRegistration: ValidIncorporatedEntityRegistration) =>
        val validContacts       = validRegistration.registration.contacts
        val invalidRegistration = validRegistration.registration.copy(contacts =
          validContacts.copy(secondContact = Some(true), secondContactDetails = ContactDetails(None, None, None, None))
        )

        val expectedErrors = Seq(
          DataValidationError(DataMissing, "Second contact name is missing"),
          DataValidationError(DataMissing, "Second contact role is missing"),
          DataValidationError(DataMissing, "Second contact email is missing"),
          DataValidationError(DataMissing, "Second contact number is missing")
        )

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
    }

    "return an error if the registration data contains the AML regulated activity choice as false" in forAll {
      validRegistration: ValidIncorporatedEntityRegistration =>
        val invalidRegistration = validRegistration.registration
          .copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(false))

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain only DataValidationError(
            DataInvalid,
            "Carried out AML regulated activity cannot be false"
          )
        )
    }

    "return an error if the registration data contains GC or FCA as the AML supervisor" in forAll(
      Arbitrary.arbitrary[ValidIncorporatedEntityRegistration],
      Gen.oneOf(GamblingCommission, FinancialConductAuthority)
    ) { (validRegistration: ValidIncorporatedEntityRegistration, gcOrFca: AmlSupervisorType) =>
      val invalidRegistration = validRegistration.registration
        .copy(amlSupervisor = Some(AmlSupervisor(gcOrFca, None)))

      val result = service.validateRegistration(invalidRegistration)

      result.isValid shouldBe false
      result.leftMap(nec =>
        nec.toList should contain only DataValidationError(
          DataInvalid,
          "AML supervisor cannot be GC or FCA"
        )
      )
    }

    "return an error if the registration data contains the revenue meets threshold flag as false" in forAll {
      validRegistration: ValidIncorporatedEntityRegistration =>
        val invalidRegistration = validRegistration.registration
          .copy(revenueMeetsThreshold = Some(false))

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain only DataValidationError(
            DataInvalid,
            "Revenue does not meet the liability threshold"
          )
        )
    }

    "return an error if the contact address contains no address lines" in forAll {
      validRegistration: ValidIncorporatedEntityRegistration =>
        val invalidRegistration = validRegistration.registration
          .copy(contactAddress =
            validRegistration.registration.contactAddress.map(_.copy(None, None, None, None, None, None, None, None))
          )

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain only DataValidationError(
            DataInvalid,
            "Contact address has no address lines"
          )
        )
    }

    "return errors if the registration data contains no partnership name, SA UTR and " +
      "postcode when the entity type is general or scottish partnership" in forAll {
        (validScottishOrGeneralPartnershipRegistration: ValidScottishOrGeneralPartnershipRegistration) =>
          val invalidPartnershipData =
            validScottishOrGeneralPartnershipRegistration.registration.partnershipEntityJourneyData.map(
              _.copy(
                sautr = None,
                postcode = None
              )
            )

          val invalidRegistration = validScottishOrGeneralPartnershipRegistration.registration
            .copy(partnershipEntityJourneyData = invalidPartnershipData, partnershipName = None)

          val result = service.validateRegistration(invalidRegistration)

          val expectedErrors = Seq(
            DataValidationError(DataMissing, "Partnership SA UTR is missing"),
            DataValidationError(DataMissing, "Partnership postcode is missing"),
            DataValidationError(DataMissing, "Partnership name is missing")
          )

          result.isValid shouldBe false
          result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
      }

    "return errors if the registration data contains no partnership SA UTR and " +
      "company profile when the entity type is limited, limited liability or scottish limited partnership" in forAll {
        (validLimitedPartnershipRegistration: ValidLimitedPartnershipRegistration) =>
          val invalidPartnershipData =
            validLimitedPartnershipRegistration.registration.partnershipEntityJourneyData.map(
              _.copy(
                sautr = None,
                companyProfile = None
              )
            )

          val invalidRegistration = validLimitedPartnershipRegistration.registration
            .copy(partnershipEntityJourneyData = invalidPartnershipData)

          val result = service.validateRegistration(invalidRegistration)

          val expectedErrors = Seq(
            DataValidationError(DataMissing, "Partnership SA UTR is missing"),
            DataValidationError(DataMissing, "Partnership company profile is missing")
          )

          result.isValid shouldBe false
          result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
      }

    "return errors if the registration data contains no sole trader SA UTR or NINO and the entity type is sole trader" in forAll {
      (validSoleTraderRegistration: ValidSoleTraderRegistration) =>
        val invalidSoleTraderData = validSoleTraderRegistration.registration.soleTraderEntityJourneyData.map(
          _.copy(
            sautr = None,
            nino = None
          )
        )

        val invalidRegistration =
          validSoleTraderRegistration.registration.copy(soleTraderEntityJourneyData = invalidSoleTraderData)

        val result = service.validateRegistration(invalidRegistration)

        result.isValid shouldBe false
        result.leftMap(nec =>
          nec.toList should contain only DataValidationError(
            DataMissing,
            "Sole trader SA UTR or NINO is missing"
          )
        )
    }

    "return errors if the registration for a other entity is invalid" in {
      val invalidRegistration = Registration
        .empty("")
        .copy(
          entityType = Some(Other)
        )
      val expectedErrors      = Seq(
        DataValidationError(DataMissing, "Carried out AML regulated activity choice is missing"),
        DataValidationError(DataMissing, "Relevant AP 12 months choice is missing"),
        DataValidationError(DataMissing, "Relevant AP revenue is missing"),
        DataValidationError(DataMissing, "Revenue meets threshold flag is missing"),
        DataValidationError(DataMissing, "AML supervisor is missing"),
        DataValidationError(DataMissing, "Business sector is missing"),
        DataValidationError(DataMissing, "First contact name is missing"),
        DataValidationError(DataMissing, "First contact role is missing"),
        DataValidationError(DataMissing, "First contact email is missing"),
        DataValidationError(DataMissing, "First contact number is missing"),
        DataValidationError(DataMissing, "Contact address is missing"),
        DataValidationError(DataMissing, "Second contact choice is missing"),
        DataValidationError(DataMissing, "Other entity type is missing"),
        DataValidationError(DataMissing, "Other entity data is missing"),
        DataValidationError(DataMissing, "Business name is missing")
      )
      val result              = service.validateRegistration(invalidRegistration)
      result.isValid shouldBe false
      result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
    }

    "return the registration if the registration for a charity is valid" in forAll {
      (validCharityRegistration: ValidCharityRegistration) =>
        val result = service.validateRegistration(validCharityRegistration.registration)
        result shouldBe Valid(Right(validCharityRegistration.registration))
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
        val expectedErrors             = Seq(
          DataValidationError(DataMissing, "Charity registration number is missing"),
          DataValidationError(DataMissing, "Company registration number is missing")
        )
        val result                     = service.validateRegistration(invalidCharityRegistration)
        result.isValid shouldBe false
        result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
    }

    "return errors if the registration for a unincorporated association is invalid when isCtUtrPresent flag is not present" in forAll {
      (unincorporatedAssociationRegistration: UnincorporatedAssociationRegistration) =>
        val otherEntityJourneyData                       = unincorporatedAssociationRegistration.registration.otherEntityJourneyData.copy(
          isCtUtrPresent = None,
          ctUtr = None
        )
        val invalidUnincorporatedAssociationRegistration = unincorporatedAssociationRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )
        val expectedErrors                               = Seq(
          DataValidationError(DataMissing, "Presence of corporation Tax Unique Taxpayer Reference is missing")
        )
        val result                                       = service.validateRegistration(invalidUnincorporatedAssociationRegistration)
        result.isValid shouldBe false
        result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
    }

    "return errors if the registration for a unincorporated association is invalid when CT-UTR is not present" in forAll {
      (unincorporatedAssociationRegistration: UnincorporatedAssociationRegistration) =>
        val otherEntityJourneyData                       = unincorporatedAssociationRegistration.registration.otherEntityJourneyData.copy(
          isCtUtrPresent = Some(true),
          ctUtr = None
        )
        val invalidUnincorporatedAssociationRegistration = unincorporatedAssociationRegistration.registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )
        val expectedErrors                               = Seq(
          DataValidationError(DataMissing, "Corporation Tax Unique Taxpayer Reference is missing")
        )

        val result = service.validateRegistration(invalidUnincorporatedAssociationRegistration)
        result.isValid shouldBe false
        result.leftMap(nec => nec.toList should contain theSameElementsAs expectedErrors)
    }
  }
}
