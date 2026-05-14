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

import cats.implicits.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.cats.implicits.*
import play.api.libs.json.JsObject
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, LoginTimes}
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData
import uk.gov.hmrc.economiccrimelevyregistration.models.*
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RequestStatus
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.{DeregisterReason, Deregistration}
import uk.gov.hmrc.economiccrimelevyregistration.models.dms.{DmsNotification, SubmissionItemStatus}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.*
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.ErrorCode
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.*
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.*
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs.*
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.{Instant, LocalDate}
import org.bson.types.ObjectId

object CachedArbitraries extends EclTestData {

  private def nonEmptyString: Gen[String]    = Gen.alphaNumStr.suchThat(_.nonEmpty)
  private def optString: Gen[Option[String]] = Gen.option(nonEmptyString)
  private def genInstant: Gen[Instant]       =
    Gen.long.map(l => Instant.ofEpochMilli(math.abs(l) % System.currentTimeMillis()))
  private def genLocalDate: Gen[LocalDate]   = Gen.choose(0L, 365L * 50).map(LocalDate.ofEpochDay)

  implicit lazy val arbChannel: Arbitrary[Channel] = Arbitrary(Gen.oneOf(Channel.Online, Channel.Offline))

  implicit lazy val arbAmlSupervisorType: Arbitrary[AmlSupervisorType] = Arbitrary(
    Gen.oneOf(
      AmlSupervisorType.FinancialConductAuthority,
      AmlSupervisorType.Hmrc,
      AmlSupervisorType.GamblingCommission,
      AmlSupervisorType.Other,
      AmlSupervisorType.Unknown
    )
  )

  implicit lazy val arbBusinessSector: Arbitrary[BusinessSector] = Arbitrary(
    Gen.oneOf(
      BusinessSector.Auditor,
      BusinessSector.CreditInstitution,
      BusinessSector.CryptoAssetExchangeProvider,
      BusinessSector.EstateAgentOrLettingAgent,
      BusinessSector.ExternalAccountant,
      BusinessSector.FinancialInstitution,
      BusinessSector.HighValueDealer,
      BusinessSector.IndependentLegalProfessional,
      BusinessSector.InsolvencyPractitioner,
      BusinessSector.TaxAdviser,
      BusinessSector.TrustOrCompanyServiceProvider
    )
  )

  implicit lazy val arbEntityType: Arbitrary[EntityType] = Arbitrary(
    Gen.oneOf(
      EntityType.Charity,
      EntityType.GeneralPartnership,
      EntityType.LimitedLiabilityPartnership,
      EntityType.LimitedPartnership,
      EntityType.NonUKEstablishment,
      EntityType.RegisteredSociety,
      EntityType.ScottishLimitedPartnership,
      EntityType.ScottishPartnership,
      EntityType.SoleTrader,
      EntityType.Trust,
      EntityType.UnlimitedCompany,
      EntityType.UkLimitedCompany,
      EntityType.UnincorporatedAssociation
    )
  )

  implicit lazy val arbSubscriptionStatus: Arbitrary[SubscriptionStatus] = Arbitrary(
    Gen.oneOf(
      Gen.const(EclSubscriptionStatus.NotSubscribed),
      nonEmptyString.map(EclSubscriptionStatus.Subscribed(_)),
      nonEmptyString.map(EclSubscriptionStatus.DeRegistered(_))
    )
  )

  implicit lazy val arbRegistrationStatus: Arbitrary[RegistrationStatus] = Arbitrary(
    Gen.oneOf(
      RegistrationStatus.Registered,
      RegistrationStatus.RegistrationFailed,
      RegistrationStatus.RegistrationNotCalled
    )
  )

  implicit lazy val arbVerificationStatus: Arbitrary[VerificationStatus] = Arbitrary(
    Gen.oneOf(
      VerificationStatus.CtEnrolled,
      VerificationStatus.Fail,
      VerificationStatus.Pass,
      VerificationStatus.SaEnrolled
    )
  )

  implicit lazy val arbEtmpSubscriptionStatus: Arbitrary[EtmpSubscriptionStatus] = Arbitrary(
    Gen.oneOf(
      EtmpSubscriptionStatus.ApprovedWithConditions,
      EtmpSubscriptionStatus.ContractObjectInactive,
      EtmpSubscriptionStatus.CreateFailed,
      EtmpSubscriptionStatus.DeRegistered,
      EtmpSubscriptionStatus.DsOutcomeInProgress,
      EtmpSubscriptionStatus.InProcessing,
      EtmpSubscriptionStatus.NoFormBundleFound,
      EtmpSubscriptionStatus.RegFormReceived
    )
  )

  implicit lazy val arbUtrType: Arbitrary[UtrType] = Arbitrary(Gen.oneOf(UtrType.CtUtr, UtrType.SaUtr))

  implicit lazy val arbDeregisterReason: Arbitrary[DeregisterReason] = Arbitrary(
    Gen.oneOf(
      DeregisterReason.NoAmlActivity,
      DeregisterReason.NoLongerMeetsThreshold,
      DeregisterReason.RegulatedByFcaOrGa
    )
  )

  implicit lazy val arbErrorCode: Arbitrary[ErrorCode] = Arbitrary(
    Gen.oneOf(
      ErrorCode.BadGateway,
      ErrorCode.BadRequest,
      ErrorCode.InternalServerError,
      ErrorCode.NotFound,
      ErrorCode.Unauthorized
    )
  )

  implicit lazy val arbSubmissionItemStatus: Arbitrary[SubmissionItemStatus] = Arbitrary(
    Gen.oneOf(
      SubmissionItemStatus.Completed,
      SubmissionItemStatus.Failed,
      SubmissionItemStatus.Forwarded,
      SubmissionItemStatus.Processed
    )
  )

  implicit lazy val arbRegistrationType: Arbitrary[RegistrationType] = Arbitrary(
    Gen.oneOf(RegistrationType.Initial, RegistrationType.Amendment, RegistrationType.DeRegistration)
  )

  implicit lazy val arbRequestStatus: Arbitrary[RequestStatus] = Arbitrary(
    Gen.oneOf(RequestStatus.Success, RequestStatus.Failed)
  )

  implicit lazy val arbKeyValue: Arbitrary[KeyValue] = Arbitrary(
    (nonEmptyString, nonEmptyString).mapN(KeyValue.apply)
  )

  implicit lazy val arbCreateEnrolmentRequest: Arbitrary[CreateEnrolmentRequest] = Arbitrary(
    (Gen.listOfN(2, arbKeyValue.arbitrary), Gen.listOfN(2, arbKeyValue.arbitrary)).mapN(CreateEnrolmentRequest.apply)
  )

  implicit lazy val arbUpsertKnownFactsRequest: Arbitrary[UpsertKnownFactsRequest] = Arbitrary(
    Gen.listOfN(2, arbKeyValue.arbitrary).map(UpsertKnownFactsRequest(_))
  )

  implicit lazy val arbKnownFactsWorkItem: Arbitrary[KnownFactsWorkItem] = Arbitrary(
    (nonEmptyString, nonEmptyString).mapN(KnownFactsWorkItem.apply)
  )

  implicit lazy val arbNrsSubmissionResponse: Arbitrary[NrsSubmissionResponse] = Arbitrary(
    nonEmptyString.map(NrsSubmissionResponse(_))
  )

  implicit lazy val arbCreateEclSubscriptionResponsePayload: Arbitrary[CreateEclSubscriptionResponsePayload] =
    Arbitrary(
      (genInstant, nonEmptyString).mapN(CreateEclSubscriptionResponsePayload.apply)
    )

  implicit lazy val arbCreateEclSubscriptionResponse: Arbitrary[CreateEclSubscriptionResponse] = Arbitrary(
    arbCreateEclSubscriptionResponsePayload.arbitrary.map(CreateEclSubscriptionResponse(_))
  )

  implicit lazy val arbEclSubscriptionStatus: Arbitrary[EclSubscriptionStatus] = Arbitrary(
    arbSubscriptionStatus.arbitrary.map(EclSubscriptionStatus(_))
  )

  implicit lazy val arbIncorporatedEntityAddress: Arbitrary[IncorporatedEntityAddress] = Arbitrary(
    (optString, optString, optString, optString, optString, optString, optString, optString)
      .mapN(IncorporatedEntityAddress.apply)
  )

  implicit lazy val arbCompanyProfile: Arbitrary[CompanyProfile] = Arbitrary(
    (
      nonEmptyString,
      nonEmptyString,
      optString,
      arbIncorporatedEntityAddress.arbitrary
    ).mapN(CompanyProfile.apply)
  )

  implicit lazy val arbFullName: Arbitrary[FullName] = Arbitrary(
    (nonEmptyString, nonEmptyString).mapN(FullName.apply)
  )

  implicit lazy val arbGrsRegistrationResultFailures: Arbitrary[GrsRegistrationResultFailures] = Arbitrary(
    (nonEmptyString, nonEmptyString).mapN(GrsRegistrationResultFailures.apply)
  )

  implicit lazy val arbGrsRegistrationResult: Arbitrary[GrsRegistrationResult] = Arbitrary(
    (
      arbRegistrationStatus.arbitrary,
      optString,
      Gen.option(Gen.listOfN(1, arbGrsRegistrationResultFailures.arbitrary).map(_.toSeq))
    ).mapN(GrsRegistrationResult.apply)
  )

  implicit lazy val arbBusinessVerificationResult: Arbitrary[BusinessVerificationResult] = Arbitrary(
    arbVerificationStatus.arbitrary.map(BusinessVerificationResult(_))
  )

  implicit lazy val arbIncorporatedEntityJourneyData: Arbitrary[IncorporatedEntityJourneyData] = Arbitrary(
    (
      arbCompanyProfile.arbitrary,
      nonEmptyString,
      arbitrary[Boolean],
      Gen.option(arbBusinessVerificationResult.arbitrary),
      arbGrsRegistrationResult.arbitrary
    ).mapN(IncorporatedEntityJourneyData.apply)
  )

  implicit lazy val arbPartnershipEntityJourneyData: Arbitrary[PartnershipEntityJourneyData] = Arbitrary(
    (
      optString,
      optString,
      arbitrary[Boolean],
      Gen.option(arbBusinessVerificationResult.arbitrary),
      arbGrsRegistrationResult.arbitrary,
      Gen.option(arbCompanyProfile.arbitrary)
    ).mapN(PartnershipEntityJourneyData.apply)
  )

  implicit lazy val arbSoleTraderEntityJourneyData: Arbitrary[SoleTraderEntityJourneyData] = Arbitrary(
    (
      arbFullName.arbitrary,
      genLocalDate,
      optString,
      optString,
      arbitrary[Boolean],
      Gen.option(arbBusinessVerificationResult.arbitrary),
      arbGrsRegistrationResult.arbitrary
    ).mapN(SoleTraderEntityJourneyData.apply)
  )

  implicit lazy val arbEclAddress: Arbitrary[EclAddress] = Arbitrary(
    (
      optString,
      optString,
      optString,
      optString,
      optString,
      optString,
      optString,
      optString,
      Gen.oneOf("GB", "US", "FR", "DE")
    ).mapN(EclAddress.apply)
  )

  implicit lazy val arbContactDetails: Arbitrary[ContactDetails] = Arbitrary(
    (optString, optString, optString, optString).mapN(ContactDetails.apply)
  )

  implicit lazy val arbContacts: Arbitrary[Contacts] = Arbitrary(
    (
      arbContactDetails.arbitrary,
      Gen.option(arbitrary[Boolean]),
      arbContactDetails.arbitrary
    ).mapN(Contacts.apply)
  )

  implicit lazy val arbAmlSupervisor: Arbitrary[AmlSupervisor] = Arbitrary(
    (arbAmlSupervisorType.arbitrary, optString).mapN(AmlSupervisor.apply)
  )

  implicit lazy val arbBase64EncodedFields: Arbitrary[Base64EncodedFields] = Arbitrary(
    (optString, optString).mapN(Base64EncodedFields.apply)
  )

  implicit lazy val arbOtherEntityJourneyData: Arbitrary[OtherEntityJourneyData] = Arbitrary(
    (
      optString,
      optString,
      optString,
      Gen.option(arbUtrType.arbitrary),
      optString,
      Gen.option(arbitrary[Boolean]),
      optString,
      optString,
      Gen.option(arbitrary[Boolean])
    ).mapN(OtherEntityJourneyData.apply)
  )

  implicit lazy val arbRegistration: Arbitrary[Registration] = Arbitrary(
    (
      nonEmptyString,
      Gen.option(arbitrary[Boolean]),
      Gen.option(arbAmlSupervisor.arbitrary),
      Gen.option(arbitrary[Boolean]),
      Gen.option(Gen.choose(1, 365)),
      Gen.option(Gen.chooseNum[Long](0, 10000000000L).map(n => BigDecimal(n, 2))),
      Gen.option(arbitrary[Boolean]),
      Gen.option(arbEntityType.arbitrary),
      Gen.option(arbIncorporatedEntityJourneyData.arbitrary),
      Gen.option(arbSoleTraderEntityJourneyData.arbitrary),
      Gen.option(arbPartnershipEntityJourneyData.arbitrary),
      Gen.option(arbBusinessSector.arbitrary),
      arbContacts.arbitrary,
      Gen.option(arbitrary[Boolean]),
      Gen.option(arbEclAddress.arbitrary),
      Gen.option(arbitrary[Boolean]),
      optString,
      Gen.option(arbBase64EncodedFields.arbitrary),
      Gen.option(arbOtherEntityJourneyData.arbitrary),
      Gen.option(genInstant),
      Gen.option(arbRegistrationType.arbitrary),
      optString
    ).mapN(Registration.apply)
  )

  implicit lazy val arbRegistrationAdditionalInfo: Arbitrary[RegistrationAdditionalInfo] = Arbitrary(
    (
      nonEmptyString,
      Gen.option(Gen.choose(2020, 2030)),
      optString,
      Gen.option(genLocalDate),
      Gen.option(arbitrary[Boolean]),
      Gen.option(arbitrary[Boolean]),
      Gen.option(genInstant)
    ).mapN(RegistrationAdditionalInfo.apply)
  )

  implicit lazy val arbSessionData: Arbitrary[SessionData] = Arbitrary(
    (
      nonEmptyString,
      Gen.mapOfN(3, (nonEmptyString, nonEmptyString).tupled),
      Gen.option(genInstant)
    ).mapN(SessionData.apply)
  )

  implicit lazy val arbEclRegistrationModel: Arbitrary[EclRegistrationModel] = Arbitrary(
    (
      arbRegistration.arbitrary,
      Gen.option(arbRegistrationAdditionalInfo.arbitrary)
    ).mapN(EclRegistrationModel.apply)
  )

  implicit lazy val arbSubscriptionStatusResponse: Arbitrary[SubscriptionStatusResponse] = Arbitrary(
    (
      arbEtmpSubscriptionStatus.arbitrary,
      optString,
      optString,
      Gen.option(arbChannel.arbitrary)
    ).mapN(SubscriptionStatusResponse.apply)
  )

  implicit lazy val arbDmsNotification: Arbitrary[DmsNotification] = Arbitrary(
    (nonEmptyString, arbSubmissionItemStatus.arbitrary, optString).mapN(DmsNotification.apply)
  )

  implicit lazy val arbDeregistration: Arbitrary[Deregistration] = Arbitrary(
    (
      nonEmptyString,
      optString,
      Gen.option(arbDeregisterReason.arbitrary),
      Gen.option(genLocalDate),
      arbContactDetails.arbitrary,
      arbRegistrationType.arbitrary,
      Gen.option(genInstant),
      optString
    ).mapN(Deregistration.apply)
  )

  implicit lazy val arbNrsSearchKeys: Arbitrary[NrsSearchKeys] = Arbitrary(
    nonEmptyString.map(NrsSearchKeys(_))
  )

  implicit lazy val arbNrsIdentityData: Arbitrary[NrsIdentityData] = Arbitrary(
    (
      nonEmptyString,
      optString,
      optString,
      Gen.const(None),
      Gen.choose(50, 500),
      optString,
      optString,
      Gen.const(None),
      Gen.option(genLocalDate),
      optString,
      Gen.const(AgentInformation(None, None, None)),
      optString,
      Gen.const(None),
      Gen.const(None),
      Gen.const(None),
      Gen.option(genLocalDate),
      Gen.const(None),
      Gen.const(None),
      optString,
      genInstant.map(i => LoginTimes(i, None))
    ).mapN(NrsIdentityData.apply)
  )

  implicit lazy val arbNrsMetadata: Arbitrary[NrsMetadata] = Arbitrary(
    (
      nonEmptyString,
      nonEmptyString,
      Gen.const("application/json"),
      nonEmptyString,
      genInstant,
      arbNrsIdentityData.arbitrary,
      nonEmptyString,
      Gen.const(JsObject.empty),
      arbNrsSearchKeys.arbitrary
    ).mapN(NrsMetadata.apply)
  )

  implicit lazy val arbNrsSubmission: Arbitrary[NrsSubmission] = Arbitrary(
    (nonEmptyString, arbNrsMetadata.arbitrary).mapN(NrsSubmission.apply)
  )

  implicit lazy val arbEitherErrorOrHttpResponse: Arbitrary[Either[UpstreamErrorResponse, HttpResponse]] = Arbitrary(
    Gen.oneOf(
      Gen.choose(400, 599).map(code => Left(UpstreamErrorResponse("error", code))),
      Gen.choose(200, 299).map(code => Right(HttpResponse(code, "")))
    )
  )

  implicit lazy val arbWorkItemKnownFactsWorkItem: Arbitrary[WorkItem[KnownFactsWorkItem]] = Arbitrary(
    (
      arbKnownFactsWorkItem.arbitrary,
      genInstant,
      genInstant,
      Gen.oneOf(
        ProcessingStatus.ToDo,
        ProcessingStatus.InProgress,
        ProcessingStatus.Failed,
        ProcessingStatus.PermanentlyFailed
      ),
      Gen.choose(0, 5)
    ).mapN { (item, receivedAt, updatedAt, status, failureCount) =>
      WorkItem(
        id = ObjectId.get(),
        receivedAt = receivedAt,
        updatedAt = updatedAt,
        availableAt = receivedAt,
        status = status,
        failureCount = failureCount,
        item = item
      )
    }
  )

  implicit lazy val arbAuditResult: Arbitrary[AuditResult] = Arbitrary(
    Gen.oneOf(AuditResult.Success, AuditResult.Disabled)
  )

  implicit lazy val arbSubscriptionContactDetails: Arbitrary[SubscriptionContactDetails] = Arbitrary(
    (nonEmptyString, nonEmptyString, nonEmptyString, nonEmptyString).mapN(SubscriptionContactDetails.apply)
  )

  implicit lazy val arbCorrespondenceAddressDetails: Arbitrary[CorrespondenceAddressDetails] = Arbitrary(
    (nonEmptyString, optString, optString, optString, optString, Gen.option(Gen.oneOf("GB", "US", "FR", "DE")))
      .mapN(CorrespondenceAddressDetails.apply)
  )

  implicit lazy val arbLegalEntityDetails: Arbitrary[LegalEntityDetails] = Arbitrary(
    (
      nonEmptyString,
      optString,
      optString,
      optString,
      optString,
      nonEmptyString,
      nonEmptyString,
      nonEmptyString,
      nonEmptyString,
      nonEmptyString
    ).mapN(LegalEntityDetails.apply)
  )

  implicit lazy val arbSubscription: Arbitrary[Subscription] = Arbitrary(
    (
      arbLegalEntityDetails.arbitrary,
      arbCorrespondenceAddressDetails.arbitrary,
      arbSubscriptionContactDetails.arbitrary,
      Gen.option(arbSubscriptionContactDetails.arbitrary)
    ).mapN(Subscription.apply)
  )

  implicit lazy val arbEclSubscription: Arbitrary[EclSubscription] = Arbitrary(
    (nonEmptyString, arbSubscription.arbitrary).mapN(EclSubscription.apply)
  )

  implicit lazy val arbGetPrimaryContactDetails: Arbitrary[GetPrimaryContactDetails] = Arbitrary(
    (nonEmptyString, nonEmptyString, nonEmptyString, nonEmptyString).mapN(GetPrimaryContactDetails.apply)
  )

  implicit lazy val arbGetCorrespondenceAddressDetails: Arbitrary[GetCorrespondenceAddressDetails] = Arbitrary(
    (nonEmptyString, optString, optString, optString, optString, optString).mapN(GetCorrespondenceAddressDetails.apply)
  )

  implicit lazy val arbGetLegalEntityDetails: Arbitrary[GetLegalEntityDetails] = Arbitrary(
    (nonEmptyString, optString, nonEmptyString, optString, optString, optString).mapN(GetLegalEntityDetails.apply)
  )

  implicit lazy val arbGetAdditionalDetails: Arbitrary[GetAdditionalDetails] = Arbitrary(
    (nonEmptyString, nonEmptyString, nonEmptyString, nonEmptyString, nonEmptyString).mapN(GetAdditionalDetails.apply)
  )

  implicit lazy val arbGetSubscriptionResponse: Arbitrary[GetSubscriptionResponse] = Arbitrary(
    (
      nonEmptyString,
      arbGetLegalEntityDetails.arbitrary,
      arbGetCorrespondenceAddressDetails.arbitrary,
      arbGetPrimaryContactDetails.arbitrary,
      Gen.const(None),
      arbGetAdditionalDetails.arbitrary
    ).mapN(GetSubscriptionResponse.apply)
  )
}
