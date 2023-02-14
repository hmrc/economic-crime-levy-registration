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
import org.scalacheck.derive.MkArbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.Hmrc
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.IncorporatedEntityJourneyData
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EtmpSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{Channel, EtmpSubscriptionStatus, SubscriptionStatusResponse}

import java.time.{Instant, LocalDate}

final case class ValidRegistration(registration: Registration, expectedBusinessPartnerId: String)

final case class PartnershipType(entityType: EntityType)

final case class ScottishOrGeneralPartnershipType(entityType: EntityType)

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

  implicit val arbValidRegistration: Arbitrary[ValidRegistration] = Arbitrary {
    for {
      registration                  <- MkArbitrary[Registration].arbitrary.arbitrary
      internalId                     = alphaNumericString
      incorporatedEntityJourneyData <- Arbitrary.arbitrary[IncorporatedEntityJourneyData]
      businessPartnerId              = alphaNumericString
      businessSector                <- Arbitrary.arbitrary[BusinessSector]
      firstContactName              <- Arbitrary.arbitrary[String]
      firstContactRole              <- Arbitrary.arbitrary[String]
      firstContactEmail             <- Arbitrary.arbitrary[String]
      firstContactNumber            <- Arbitrary.arbitrary[String]
      eclAddress                    <- Arbitrary.arbitrary[EclAddress]
      relevantAp12Months            <- Arbitrary.arbitrary[Boolean]
      relevantApLength              <- Arbitrary.arbitrary[Int]
      relevantApRevenue             <- Arbitrary.arbitrary[Long]
    } yield ValidRegistration(
      registration.copy(
        internalId = internalId,
        entityType = Some(UkLimitedCompany),
        carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
        relevantAp12Months = Some(relevantAp12Months),
        relevantApLength = if (relevantAp12Months) None else Some(relevantApLength),
        relevantApRevenue = Some(relevantApRevenue),
        revenueMeetsThreshold = Some(true),
        incorporatedEntityJourneyData = Some(
          incorporatedEntityJourneyData.copy(registration =
            incorporatedEntityJourneyData.registration.copy(registeredBusinessPartnerId = Some(businessPartnerId))
          )
        ),
        partnershipEntityJourneyData = None,
        soleTraderEntityJourneyData = None,
        amlSupervisor = Some(AmlSupervisor(Hmrc, None)),
        businessSector = Some(businessSector),
        contacts = Contacts.empty.copy(
          firstContactDetails = ContactDetails(
            name = Some(firstContactName),
            role = Some(firstContactRole),
            emailAddress = Some(firstContactEmail),
            telephoneNumber = Some(firstContactNumber)
          ),
          secondContact = Some(false)
        ),
        contactAddress = Some(eclAddress)
      ),
      businessPartnerId
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

  def alphaNumericString: String = Gen.alphaNumStr.retryUntil(_.nonEmpty).sample.get

  val testBusinessPartnerId: String                = alphaNumericString
  val testEclRegistrationReference: String         = alphaNumericString
  val testOtherRegimeRegistrationReference: String = alphaNumericString
}
