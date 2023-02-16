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

package uk.gov.hmrc.economiccrimelevyregistration.generators

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.Arbitrary
import org.scalacheck.derive.MkArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisorType, BusinessSector, EclAddress, EntityType, SubscriptionStatus}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, RegistrationStatus, SoleTraderEntityJourneyData, VerificationStatus}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{Channel, CreateEclSubscriptionResponse, EclSubscription, EtmpSubscriptionStatus}

object CachedArbitraries extends EclTestData {

  private def mkArb[T](implicit mkArb: MkArbitrary[T]): Arbitrary[T] = MkArbitrary[T].arbitrary

  implicit lazy val arbChannel: Arbitrary[Channel]                                             = mkArb
  implicit lazy val arbCreateEnrolmentRequest: Arbitrary[CreateEnrolmentRequest]               = mkArb
  implicit lazy val arbAmlSupervisorType: Arbitrary[AmlSupervisorType]                         = mkArb
  implicit lazy val arbBusinessSector: Arbitrary[BusinessSector]                               = mkArb
  implicit lazy val arbEntityType: Arbitrary[EntityType]                                       = mkArb
  implicit lazy val arbSubscriptionStatus: Arbitrary[SubscriptionStatus]                       = mkArb
  implicit lazy val arbRegistrationStatus: Arbitrary[RegistrationStatus]                       = mkArb
  implicit lazy val arbVerificationStatus: Arbitrary[VerificationStatus]                       = mkArb
  implicit lazy val arbEtmpSubscriptionStatus: Arbitrary[EtmpSubscriptionStatus]               = mkArb
  implicit lazy val arbIncorporatedEntityJourneyData: Arbitrary[IncorporatedEntityJourneyData] = mkArb
  implicit lazy val arbPartnershipEntityJourneyData: Arbitrary[PartnershipEntityJourneyData]   = mkArb
  implicit lazy val arbSoleTraderEntityJourneyData: Arbitrary[SoleTraderEntityJourneyData]     = mkArb
  implicit lazy val arbEclAddress: Arbitrary[EclAddress]                                       = mkArb
  implicit lazy val arbCreateEclSubscriptionResponse: Arbitrary[CreateEclSubscriptionResponse] = mkArb
  implicit lazy val arbEclSubscription: Arbitrary[EclSubscription]                             = mkArb

}
