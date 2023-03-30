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

package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.bson.types.ObjectId
import org.scalacheck.Gen.{choose, listOfN}
import org.scalacheck.derive.MkArbitrary
import org.scalacheck.{Arbitrary, Gen}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.Hmrc
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{CompanyProfile, IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EtmpSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.LegalEntityDetails.CustomerType
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.{Instant, LocalDate}

final case class ValidUkCompanyRegistration(registration: Registration, expectedEclSubscription: EclSubscription)

final case class ValidSoleTraderRegistration(registration: Registration, expectedEclSubscription: EclSubscription)

final case class ValidLimitedPartnershipRegistration(
  registration: Registration,
  expectedEclSubscription: EclSubscription
)

final case class ValidScottishOrGeneralPartnershipRegistration(
  registration: Registration,
  expectedEclSubscription: EclSubscription
)

final case class PartnershipType(entityType: EntityType)

final case class ScottishOrGeneralPartnershipType(entityType: EntityType)

final case class LimitedPartnershipType(entityType: EntityType)

final case class CommonRegistrationData(registration: Registration)

trait EclTestData {

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbRegistration: Arbitrary[Registration] = Arbitrary {
    for {
      registration <- MkArbitrary[Registration].arbitrary.arbitrary
      internalId   <- Gen.nonEmptyListOf(Arbitrary.arbitrary[Char]).map(_.mkString)
    } yield registration.copy(internalId = internalId)
  }

  implicit val arbSubscriptionStatusResponse: Arbitrary[SubscriptionStatusResponse] = Arbitrary {
    for {
      etmpSubscriptionStatus   <- Arbitrary.arbitrary[EtmpSubscriptionStatus]
      idType                    = if (etmpSubscriptionStatus == Successful) Some("ZECL") else None
      eclRegistrationReference <- Arbitrary.arbitrary[String]
      idValue                   = if (etmpSubscriptionStatus == Successful) Some(eclRegistrationReference) else None
      channel                  <- Arbitrary.arbitrary[Option[Channel]]
    } yield SubscriptionStatusResponse(
      etmpSubscriptionStatus,
      idType,
      idValue,
      channel
    )
  }

  def alphaNumStringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars  <- listOfN(length, Gen.alphaNumChar)
    } yield chars.mkString

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars  <- listOfN(length, Arbitrary.arbitrary[Char])
    } yield chars.mkString

  def emailAddress(maxLength: Int): Gen[String] = {
    val emailPartsLength = maxLength / 5

    for {
      firstPart  <- alphaNumStringsWithMaxLength(emailPartsLength)
      secondPart <- alphaNumStringsWithMaxLength(emailPartsLength)
      thirdPart  <- alphaNumStringsWithMaxLength(emailPartsLength)
    } yield s"$firstPart@$secondPart.$thirdPart".toLowerCase
  }

  implicit def arbCommonRegistrationData: Arbitrary[CommonRegistrationData] = Arbitrary {
    for {
      businessSector     <- Arbitrary.arbitrary[BusinessSector]
      eclAddress          = EclAddress(
                              organisation = Some("Test Org Name"),
                              addressLine1 = Some("Test Address Line 1"),
                              addressLine2 = Some("Test Address Line 2"),
                              addressLine3 = None,
                              addressLine4 = None,
                              region = Some("Test Region"),
                              postCode = Some("AB12 3DE"),
                              poBox = None,
                              countryCode = "GB"
                            )
      relevantAp12Months <- Arbitrary.arbitrary[Boolean]
      relevantApLength   <- Arbitrary.arbitrary[Int]
      relevantApRevenue  <- Arbitrary.arbitrary[Long]
      firstContactName   <- stringsWithMaxLength(160)
      firstContactRole   <- stringsWithMaxLength(160)
      firstContactEmail  <- emailAddress(132)
      firstContactNumber <- telephoneNumber(24)
      internalId          = alphaNumericString
    } yield CommonRegistrationData(
      Registration
        .empty(internalId = internalId)
        .copy(
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
          businessSector = Some(businessSector),
          relevantAp12Months = Some(relevantAp12Months),
          relevantApLength = if (relevantAp12Months) None else Some(relevantApLength),
          relevantApRevenue = Some(relevantApRevenue),
          revenueMeetsThreshold = Some(true),
          contacts = Contacts.empty.copy(
            firstContactDetails = ContactDetails(
              name = Some(firstContactName),
              role = Some(firstContactRole),
              emailAddress = Some(firstContactEmail),
              telephoneNumber = Some(firstContactNumber)
            ),
            secondContact = Some(false)
          ),
          contactAddress = Some(eclAddress),
          amlSupervisor = Some(AmlSupervisor(Hmrc, None))
        )
    )
  }

  def telephoneNumber(maxLength: Int): Gen[String] =
    RegexpGen.from(s"${Regex.telephoneNumber}").retryUntil(s => s.length <= maxLength && s.trim.nonEmpty)

  private def commonCorrespondenceAddressDetails: CorrespondenceAddressDetails =
    CorrespondenceAddressDetails(
      addressLine1 = "Test Org Name",
      addressLine2 = Some("Test Address Line 1"),
      addressLine3 = Some("Test Address Line 2"),
      addressLine4 = Some("Test Region"),
      postCode = Some("AB12 3DE"),
      countryCode = Some("GB")
    )

  implicit val arbCompanyProfile: Arbitrary[CompanyProfile] = Arbitrary {
    for {
      companyProfile <- MkArbitrary[CompanyProfile].arbitrary.arbitrary
      companyName    <- stringsWithMaxLength(160)
      companyNumber  <- RegexpGen.from(Regex.customerIdentificationNumber)
    } yield companyProfile.copy(companyName = companyName, companyNumber = companyNumber)
  }

  implicit val arbValidUkCompanyRegistration: Arbitrary[ValidUkCompanyRegistration] = Arbitrary {
    for {
      businessPartnerId             <- RegexpGen.from(Regex.businessPartnerId)
      incorporatedEntityJourneyData <- Arbitrary.arbitrary[IncorporatedEntityJourneyData]
      ctutr                         <- RegexpGen.from(Regex.customerIdentificationNumber)
      commonRegistrationData        <- Arbitrary.arbitrary[CommonRegistrationData]
    } yield ValidUkCompanyRegistration(
      commonRegistrationData.registration.copy(
        entityType = Some(UkLimitedCompany),
        incorporatedEntityJourneyData = Some(
          incorporatedEntityJourneyData.copy(
            ctutr = ctutr,
            registration =
              incorporatedEntityJourneyData.registration.copy(registeredBusinessPartnerId = Some(businessPartnerId))
          )
        ),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None
      ),
      EclSubscription(
        businessPartnerId = businessPartnerId,
        subscription = Subscription(
          LegalEntityDetails(
            customerIdentification1 = ctutr,
            customerIdentification2 = Some(incorporatedEntityJourneyData.companyProfile.companyNumber),
            organisationName = Some(incorporatedEntityJourneyData.companyProfile.companyName),
            firstName = None,
            lastName = None,
            customerType = CustomerType.Organisation,
            registrationDate = "2007-12-25",
            liabilityStartDate = "2007-12-25",
            amlSupervisor = "Hmrc",
            businessSector = commonRegistrationData.registration.businessSector.get.toString
          ),
          correspondenceAddressDetails = commonCorrespondenceAddressDetails,
          primaryContactDetails = SubscriptionContactDetails(
            name = commonRegistrationData.registration.contacts.firstContactDetails.name.get,
            positionInCompany = commonRegistrationData.registration.contacts.firstContactDetails.role.get,
            telephone = commonRegistrationData.registration.contacts.firstContactDetails.telephoneNumber.get,
            emailAddress = commonRegistrationData.registration.contacts.firstContactDetails.emailAddress.get
          ),
          secondaryContactDetails = None
        )
      )
    )
  }

  implicit val arbValidSoleTraderRegistration: Arbitrary[ValidSoleTraderRegistration] = Arbitrary {
    for {
      businessPartnerId           <- RegexpGen.from(Regex.businessPartnerId)
      soleTraderEntityJourneyData <- Arbitrary.arbitrary[SoleTraderEntityJourneyData]
      sautr                       <- RegexpGen.from(Regex.customerIdentificationNumber)
      nino                        <- RegexpGen.from(Regex.customerIdentificationNumber)
      commonRegistrationData      <- Arbitrary.arbitrary[CommonRegistrationData]
    } yield ValidSoleTraderRegistration(
      commonRegistrationData.registration.copy(
        entityType = Some(SoleTrader),
        incorporatedEntityJourneyData = None,
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = Some(
          soleTraderEntityJourneyData.copy(
            sautr = Some(sautr),
            nino = Some(nino),
            registration =
              soleTraderEntityJourneyData.registration.copy(registeredBusinessPartnerId = Some(businessPartnerId))
          )
        )
      ),
      EclSubscription(
        businessPartnerId = businessPartnerId,
        subscription = Subscription(
          LegalEntityDetails(
            customerIdentification1 = sautr,
            customerIdentification2 = Some(nino),
            organisationName = None,
            firstName = Some(soleTraderEntityJourneyData.fullName.firstName),
            lastName = Some(soleTraderEntityJourneyData.fullName.lastName),
            customerType = CustomerType.Individual,
            registrationDate = "2007-12-25",
            liabilityStartDate = "2007-12-25",
            amlSupervisor = "Hmrc",
            businessSector = commonRegistrationData.registration.businessSector.get.toString
          ),
          correspondenceAddressDetails = commonCorrespondenceAddressDetails,
          primaryContactDetails = SubscriptionContactDetails(
            name = commonRegistrationData.registration.contacts.firstContactDetails.name.get,
            positionInCompany = commonRegistrationData.registration.contacts.firstContactDetails.role.get,
            telephone = commonRegistrationData.registration.contacts.firstContactDetails.telephoneNumber.get,
            emailAddress = commonRegistrationData.registration.contacts.firstContactDetails.emailAddress.get
          ),
          secondaryContactDetails = None
        )
      )
    )
  }

  implicit val arbValidLimitedPartnershipRegistration: Arbitrary[ValidLimitedPartnershipRegistration] = Arbitrary {
    for {
      businessPartnerId            <- RegexpGen.from(Regex.businessPartnerId)
      partnershipEntityJourneyData <- Arbitrary.arbitrary[PartnershipEntityJourneyData]
      companyProfile               <- Arbitrary.arbitrary[CompanyProfile]
      sautr                        <- RegexpGen.from(Regex.customerIdentificationNumber)
      commonRegistrationData       <- Arbitrary.arbitrary[CommonRegistrationData]
      partnershipType              <- Arbitrary.arbitrary[LimitedPartnershipType]
    } yield ValidLimitedPartnershipRegistration(
      commonRegistrationData.registration.copy(
        entityType = Some(partnershipType.entityType),
        incorporatedEntityJourneyData = None,
        partnershipEntityJourneyData = Some(
          partnershipEntityJourneyData.copy(
            sautr = Some(sautr),
            companyProfile = Some(companyProfile),
            registration =
              partnershipEntityJourneyData.registration.copy(registeredBusinessPartnerId = Some(businessPartnerId))
          )
        ),
        soleTraderEntityJourneyData = None
      ),
      EclSubscription(
        businessPartnerId = businessPartnerId,
        subscription = Subscription(
          LegalEntityDetails(
            customerIdentification1 = sautr,
            customerIdentification2 = Some(companyProfile.companyNumber),
            organisationName = Some(companyProfile.companyName),
            firstName = None,
            lastName = None,
            customerType = CustomerType.Organisation,
            registrationDate = "2007-12-25",
            liabilityStartDate = "2007-12-25",
            amlSupervisor = "Hmrc",
            businessSector = commonRegistrationData.registration.businessSector.get.toString
          ),
          correspondenceAddressDetails = commonCorrespondenceAddressDetails,
          primaryContactDetails = SubscriptionContactDetails(
            name = commonRegistrationData.registration.contacts.firstContactDetails.name.get,
            positionInCompany = commonRegistrationData.registration.contacts.firstContactDetails.role.get,
            telephone = commonRegistrationData.registration.contacts.firstContactDetails.telephoneNumber.get,
            emailAddress = commonRegistrationData.registration.contacts.firstContactDetails.emailAddress.get
          ),
          secondaryContactDetails = None
        )
      )
    )
  }

  implicit val arbValidScottishOrGeneralPartnershipRegistration
    : Arbitrary[ValidScottishOrGeneralPartnershipRegistration] =
    Arbitrary {
      for {
        businessPartnerId            <- RegexpGen.from(Regex.businessPartnerId)
        partnershipEntityJourneyData <- Arbitrary.arbitrary[PartnershipEntityJourneyData]
        sautr                        <- RegexpGen.from(Regex.customerIdentificationNumber)
        postcode                     <- RegexpGen.from(Regex.customerIdentificationNumber)
        commonRegistrationData       <- Arbitrary.arbitrary[CommonRegistrationData]
        partnershipType              <- Arbitrary.arbitrary[ScottishOrGeneralPartnershipType]
        partnershipName               = "Test Partnership Name"
      } yield ValidScottishOrGeneralPartnershipRegistration(
        commonRegistrationData.registration.copy(
          entityType = Some(partnershipType.entityType),
          incorporatedEntityJourneyData = None,
          partnershipEntityJourneyData = Some(
            partnershipEntityJourneyData.copy(
              sautr = Some(sautr),
              postcode = Some(postcode),
              registration =
                partnershipEntityJourneyData.registration.copy(registeredBusinessPartnerId = Some(businessPartnerId))
            )
          ),
          soleTraderEntityJourneyData = None,
          partnershipName = Some(partnershipName)
        ),
        EclSubscription(
          businessPartnerId = businessPartnerId,
          subscription = Subscription(
            LegalEntityDetails(
              customerIdentification1 = sautr,
              customerIdentification2 = Some(postcode),
              organisationName = Some(partnershipName),
              firstName = None,
              lastName = None,
              customerType = CustomerType.Organisation,
              registrationDate = "2007-12-25",
              liabilityStartDate = "2007-12-25",
              amlSupervisor = "Hmrc",
              businessSector = commonRegistrationData.registration.businessSector.get.toString
            ),
            correspondenceAddressDetails = commonCorrespondenceAddressDetails,
            primaryContactDetails = SubscriptionContactDetails(
              name = commonRegistrationData.registration.contacts.firstContactDetails.name.get,
              positionInCompany = commonRegistrationData.registration.contacts.firstContactDetails.role.get,
              telephone = commonRegistrationData.registration.contacts.firstContactDetails.telephoneNumber.get,
              emailAddress = commonRegistrationData.registration.contacts.firstContactDetails.emailAddress.get
            ),
            secondaryContactDetails = None
          )
        )
      )
    }

  implicit val arbPartnershipType: Arbitrary[PartnershipType] = Arbitrary {
    for {
      partnershipType <- Gen.oneOf(
                           Seq(
                             LimitedPartnership,
                             LimitedLiabilityPartnership,
                             GeneralPartnership,
                             ScottishPartnership,
                             ScottishLimitedPartnership
                           )
                         )
    } yield PartnershipType(partnershipType)
  }

  implicit val arbScottishOrGeneralPartnershipType: Arbitrary[ScottishOrGeneralPartnershipType] = Arbitrary {
    for {
      scottishOrGeneralPartnershipType <- Gen.oneOf(
                                            Seq(
                                              GeneralPartnership,
                                              ScottishPartnership
                                            )
                                          )
    } yield ScottishOrGeneralPartnershipType(scottishOrGeneralPartnershipType)
  }

  implicit val arbLimitedPartnershipType: Arbitrary[LimitedPartnershipType] = Arbitrary {
    for {
      limitedPartnershipType <- Gen.oneOf(
                                  Seq(
                                    LimitedPartnership,
                                    LimitedLiabilityPartnership,
                                    ScottishLimitedPartnership
                                  )
                                )
    } yield LimitedPartnershipType(limitedPartnershipType)
  }

  implicit val arbUpstreamErrorResponse: Arbitrary[UpstreamErrorResponse] = Arbitrary {
    UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
  }

  implicit val arbSuccessfulHttpResponse: Arbitrary[HttpResponse] = Arbitrary {
    HttpResponse(OK, "", Map.empty)
  }

  implicit val arbObjectId: Arbitrary[ObjectId] = Arbitrary {
    ObjectId.get()
  }

  def alphaNumericString: String = Gen.alphaNumStr.retryUntil(_.nonEmpty).sample.get

  val testBusinessPartnerId: String                = alphaNumericString
  val testEclRegistrationReference: String         = alphaNumericString
  val testOtherRegimeRegistrationReference: String = alphaNumericString
}
