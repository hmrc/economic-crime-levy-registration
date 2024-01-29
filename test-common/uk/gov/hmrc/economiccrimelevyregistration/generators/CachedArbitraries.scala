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
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RequestStatus
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.dms.DmsNotification
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework._
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.workitem.WorkItem

object CachedArbitraries extends EclTestData {

  private def mkArb[T](implicit mkArb: MkArbitrary[T]): Arbitrary[T] = MkArbitrary[T].arbitrary

  implicit lazy val arbChannel: Arbitrary[Channel]                                                       = mkArb
  implicit lazy val arbCreateEnrolmentRequest: Arbitrary[CreateEnrolmentRequest]                         = mkArb
  implicit lazy val arbAmlSupervisorType: Arbitrary[AmlSupervisorType]                                   = mkArb
  implicit lazy val arbBusinessSector: Arbitrary[BusinessSector]                                         = mkArb
  implicit lazy val arbEntityType: Arbitrary[EntityType]                                                 = mkArb
  implicit lazy val arbSubscriptionStatus: Arbitrary[SubscriptionStatus]                                 = mkArb
  implicit lazy val arbRegistrationStatus: Arbitrary[RegistrationStatus]                                 = mkArb
  implicit lazy val arbVerificationStatus: Arbitrary[VerificationStatus]                                 = mkArb
  implicit lazy val arbEtmpSubscriptionStatus: Arbitrary[EtmpSubscriptionStatus]                         = mkArb
  implicit lazy val arbIncorporatedEntityJourneyData: Arbitrary[IncorporatedEntityJourneyData]           = mkArb
  implicit lazy val arbPartnershipEntityJourneyData: Arbitrary[PartnershipEntityJourneyData]             = mkArb
  implicit lazy val arbSoleTraderEntityJourneyData: Arbitrary[SoleTraderEntityJourneyData]               = mkArb
  implicit lazy val arbEclAddress: Arbitrary[EclAddress]                                                 = mkArb
  implicit lazy val arbCreateEclSubscriptionResponse: Arbitrary[CreateEclSubscriptionResponse]           = mkArb
  implicit lazy val arbEclSubscription: Arbitrary[EclSubscription]                                       = mkArb
  implicit lazy val arbUpsertKnownFactsRequest: Arbitrary[UpsertKnownFactsRequest]                       = mkArb
  implicit lazy val arbEitherErrorOrHttpResponse: Arbitrary[Either[UpstreamErrorResponse, HttpResponse]] = mkArb
  implicit lazy val arbWorkItemKnownFactsWorkItem: Arbitrary[WorkItem[KnownFactsWorkItem]]               = mkArb
  implicit lazy val arbRequestStatus: Arbitrary[RequestStatus]                                           = mkArb
  implicit lazy val arbNrsIdentityData: Arbitrary[NrsIdentityData]                                       = mkArb
  implicit lazy val arbNrsSubmission: Arbitrary[NrsSubmission]                                           = mkArb
  implicit lazy val arbNrsSubmissionResponse: Arbitrary[NrsSubmissionResponse]                           = mkArb
  implicit lazy val arbSubcriptionStatusResponse: Arbitrary[SubscriptionStatusResponse]                  = mkArb
  implicit lazy val arbUtrType: Arbitrary[UtrType]                                                       = mkArb
  implicit lazy val arbDmsNotification: Arbitrary[DmsNotification]                                       = mkArb
  implicit lazy val arbSessionData: Arbitrary[SessionData]                                               = mkArb
  implicit lazy val arbGetSubscriptionResponse: Arbitrary[GetSubscriptionResponse]                       = mkArb

}
