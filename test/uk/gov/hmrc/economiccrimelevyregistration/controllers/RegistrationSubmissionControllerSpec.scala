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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.Play.materializer
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, DataValidationError, RegistrationError, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription}
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs.NrsSubmissionResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.{Base64EncodedFields, EntityType, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.services._

import java.util.Base64
import scala.concurrent.Future

class RegistrationSubmissionControllerSpec extends SpecBase {

  val mockRegistrationValidationService: RegistrationValidationService         = mock[RegistrationValidationService]
  val mockSubscriptionServiceService: SubscriptionService                      = mock[SubscriptionService]
  val mockRegistrationService: RegistrationService                             = mock[RegistrationService]
  val mockNrsService: NrsService                                               = mock[NrsService]
  val mockDmsService: DmsService                                               = mock[DmsService]
  val mockAuditService: AuditService                                           = mock[AuditService]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]
  val mockAppConfig: AppConfig                                                 = mock[AppConfig]

  val controller = new RegistrationSubmissionController(
    cc,
    mockRegistrationService,
    fakeAuthorisedAction,
    mockRegistrationValidationService,
    mockRegistrationAdditionalInfoService,
    mockNrsService,
    mockDmsService,
    mockAuditService,
    mockSubscriptionServiceService,
    mockAppConfig
  )

  "submitRegistration" should {
    "return 200 OK with a subscription reference number in the JSON response body when the registration data is " +
      "valid for 'Normal' entities and nrsSubmissionEnabled is enabled" in forAll(
        Arbitrary.arbitrary[Registration],
        Arbitrary.arbitrary[EntityType].retryUntil(!EntityType.isOther(_)),
        Arbitrary.arbitrary[EclSubscription],
        Arbitrary.arbitrary[CreateEclSubscriptionResponse],
        Arbitrary.arbitrary[RegistrationAdditionalInfo],
        Arbitrary.arbitrary[NrsSubmissionResponse]
      ) {
        (
          aRegistration: Registration,
          entityType: EntityType,
          eclSubscription: EclSubscription,
          subscriptionResponse: CreateEclSubscriptionResponse,
          registrationAdditionalInfo: RegistrationAdditionalInfo,
          nrsSubmissionResponse: NrsSubmissionResponse
        ) =>
          reset(mockNrsService)
          reset(mockAppConfig)

          val registration = aRegistration.copy(
            entityType = Some(entityType),
            registrationType = Some(Initial)
          )

          when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)

          when(mockRegistrationService.getRegistration(any())(any())).thenReturn(EitherT.rightT(registration))

          when(mockRegistrationValidationService.validateSubscription(any()))
            .thenReturn(EitherT.rightT(eclSubscription))

          when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
            .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

          when(mockNrsService.submitToNrs(any(), any(), any())(any(), any()))
            .thenReturn(EitherT.rightT(nrsSubmissionResponse))

          when(
            mockSubscriptionServiceService
              .subscribeToEcl(
                any(),
                any(),
                any()
              )(any())
          )
            .thenReturn(EitherT.rightT(subscriptionResponse))

          val result: Future[Result] =
            controller.submitRegistration(registration.internalId)(fakeRequest)

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)

          verify(mockNrsService, times(1)).submitToNrs(
            any(),
            any(),
            any()
          )(any(), any())
      }

    "return 200 OK with a subscription reference number in the JSON response body when the registration data " +
      "is valid for 'Normal' entities and nrsSubmissionEnabled is disabled" in forAll(
        Arbitrary.arbitrary[Registration],
        Arbitrary.arbitrary[EntityType].retryUntil(!EntityType.isOther(_)),
        Arbitrary.arbitrary[EclSubscription],
        Arbitrary.arbitrary[CreateEclSubscriptionResponse],
        Arbitrary.arbitrary[RegistrationAdditionalInfo]
      ) {
        (
          aRegistration: Registration,
          entityType: EntityType,
          eclSubscription: EclSubscription,
          subscriptionResponse: CreateEclSubscriptionResponse,
          registrationAdditionalInfo: RegistrationAdditionalInfo
        ) =>
          reset(mockNrsService)
          reset(mockAppConfig)

          val registration = aRegistration.copy(
            entityType = Some(entityType),
            registrationType = Some(Initial)
          )

          when(mockAppConfig.nrsSubmissionEnabled).thenReturn(false)

          when(mockRegistrationService.getRegistration(any())(any())).thenReturn(EitherT.rightT(registration))

          when(mockRegistrationValidationService.validateSubscription(any()))
            .thenReturn(EitherT.rightT(eclSubscription))

          when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
            .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

          when(
            mockSubscriptionServiceService
              .subscribeToEcl(
                any(),
                any(),
                any()
              )(any())
          )
            .thenReturn(EitherT.rightT(subscriptionResponse))

          val result: Future[Result] =
            controller.submitRegistration(registration.internalId)(fakeRequest)

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)

          verify(mockNrsService, times(0)).submitToNrs(
            any(),
            any(),
            any()
          )(any(), any())
      }

    "when the registration type is Initial" should {
      "return 200 OK with a subscription reference number in the JSON response body when the registration data is valid for 'Other' entities" in forAll(
        Arbitrary.arbitrary[Registration],
        Arbitrary.arbitrary[CreateEclSubscriptionResponse],
        Arbitrary.arbitrary[RegistrationAdditionalInfo]
      ) {
        (
          aRegistration: Registration,
          subscriptionResponse: CreateEclSubscriptionResponse,
          registrationAdditionalInfo: RegistrationAdditionalInfo
        ) =>
          val html         = "<html><head></head><body></body></html>"
          val registration = aRegistration.copy(
            entityType = Some(Charity),
            registrationType = Some(Initial),
            base64EncodedFields = Some(Base64EncodedFields(None, Some(Base64.getEncoder.encodeToString(html.getBytes))))
          )

          when(mockRegistrationService.getRegistration(any())(any())).thenReturn(EitherT.rightT(registration))

          when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
            .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

          when(mockRegistrationValidationService.validateRegistration(any()))
            .thenReturn(EitherT.rightT(registration))

          when(mockDmsService.submitToDms(any(), any(), any())(any()))
            .thenReturn(EitherT.rightT(subscriptionResponse.success))

          val result: Future[Result] =
            controller.submitRegistration(registration.internalId)(fakeRequest)

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)
      }

      "return 400 BAD_REQUEST with message in the JSON response body when the registration data is invalid" in forAll(
        Arbitrary.arbitrary[Registration],
        Arbitrary.arbitrary[RegistrationAdditionalInfo]
      ) {
        (
          aRegistration: Registration,
          registrationAdditionalInfo: RegistrationAdditionalInfo
        ) =>
          val registration = aRegistration.copy(
            registrationType = Some(Amendment),
            entityType = Some(Charity)
          )

          when(mockRegistrationService.getRegistration(any())(any())).thenReturn(EitherT.rightT(registration))

          when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
            .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

          when(mockRegistrationValidationService.validateRegistration(any()))
            .thenReturn(EitherT.leftT(DataValidationError.DataInvalid("Invalid data")))

          val result: Future[Result] =
            controller.submitRegistration(registration.internalId)(fakeRequest)

          status(result)        shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.toJson(
            ResponseError.badRequestError("Invalid data")
          )
      }
    }

    "when the registration type is Amendment" should {
      "return 200 OK with a subscription reference number in the JSON response body when the registration data is valid " +
        "and amend NRS is enabled" in forAll(
          Arbitrary.arbitrary[Registration],
          Arbitrary.arbitrary[CreateEclSubscriptionResponse],
          Arbitrary.arbitrary[EntityType].retryUntil(!EntityType.isOther(_)),
          Arbitrary.arbitrary[NrsSubmissionResponse]
        ) {
          (
            aRegistration: Registration,
            subscriptionResponse: CreateEclSubscriptionResponse,
            entityType: EntityType,
            nrsSubmissionResponse: NrsSubmissionResponse
          ) =>
            reset(mockNrsService)
            reset(mockAppConfig)

            when(mockAppConfig.amendRegistrationNrsEnabled).thenReturn(true)

            val html         = "<html><head></head><body></body></html>"
            val registration = aRegistration.copy(
              registrationType = Some(Amendment),
              entityType = Some(entityType),
              base64EncodedFields =
                Some(Base64EncodedFields(None, Some(Base64.getEncoder.encodeToString(html.getBytes))))
            )

            val registrationAdditionalInfo =
              RegistrationAdditionalInfo(aRegistration.internalId, None, Some("Test"), None, None, None, None)

            when(mockRegistrationService.getRegistration(any())(any()))
              .thenReturn(EitherT.rightT(registration))

            when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
              .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

            when(mockRegistrationValidationService.validateRegistration(any()))
              .thenReturn(EitherT.rightT(registration))

            when(mockDmsService.submitToDms(any(), any(), any())(any()))
              .thenReturn(EitherT.rightT(subscriptionResponse.success))

            when(mockNrsService.submitToNrs(any(), any(), any())(any(), any()))
              .thenReturn(EitherT.rightT(nrsSubmissionResponse))

            val result: Future[Result] =
              controller.submitRegistration(registration.internalId)(fakeRequest)

            status(result)        shouldBe OK
            contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)

            verify(mockNrsService, times(1)).submitToNrs(
              any(),
              any(),
              any()
            )(any(), any())
        }

      "return 200 OK with a subscription reference number in the JSON response body when the registration data is valid " +
        "and amend NRS is disabled" in forAll(
          Arbitrary.arbitrary[Registration],
          Arbitrary.arbitrary[CreateEclSubscriptionResponse]
        ) {
          (
            aRegistration: Registration,
            subscriptionResponse: CreateEclSubscriptionResponse
          ) =>
            reset(mockNrsService)
            reset(mockAppConfig)

            when(mockAppConfig.amendRegistrationNrsEnabled).thenReturn(false)

            val html         = "<html><head></head><body></body></html>"
            val registration = aRegistration.copy(
              registrationType = Some(Amendment),
              entityType = Some(Charity),
              base64EncodedFields =
                Some(Base64EncodedFields(None, Some(Base64.getEncoder.encodeToString(html.getBytes))))
            )

            val registrationAdditionalInfo =
              RegistrationAdditionalInfo(registration.internalId, None, Some("Test"), None, None, None, None)

            when(mockRegistrationService.getRegistration(any())(any()))
              .thenReturn(EitherT.rightT(registration))

            when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
              .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

            when(mockRegistrationValidationService.validateRegistration(any()))
              .thenReturn(EitherT.rightT(registration))

            when(mockDmsService.submitToDms(any(), any(), any())(any()))
              .thenReturn(EitherT.rightT(subscriptionResponse.success))

            val result: Future[Result] =
              controller.submitRegistration(registration.internalId)(fakeRequest)

            status(result)        shouldBe OK
            contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)

            verify(mockNrsService, times(0)).submitToNrs(
              any(),
              any(),
              any()
            )(any(), any())
        }

      "return 400 BAD_REQUEST with message in the JSON response body when the registration data is invalid" in forAll(
        Arbitrary.arbitrary[Registration],
        Arbitrary.arbitrary[RegistrationAdditionalInfo]
      ) {
        (
          aRegistration: Registration,
          registrationAdditionalInfo: RegistrationAdditionalInfo
        ) =>
          val registration = aRegistration.copy(
            registrationType = Some(Amendment),
            entityType = Some(Charity)
          )

          when(mockRegistrationService.getRegistration(any())(any())).thenReturn(EitherT.rightT(registration))

          when(mockRegistrationValidationService.validateRegistration(any()))
            .thenReturn(EitherT.leftT(DataValidationError.DataInvalid("Invalid data")))

          when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
            .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

          val result: Future[Result] =
            controller.submitRegistration(registration.internalId)(fakeRequest)

          status(result)        shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.toJson(
            ResponseError.badGateway("Invalid data", BAD_GATEWAY)
          )

      }

      "return 500 INTERNAL_SERVER_ERROR with message in the JSON response body when the registration additional info data is invalid" in forAll(
        Arbitrary.arbitrary[Registration],
        Arbitrary.arbitrary[CreateEclSubscriptionResponse]
      ) {
        (
          aRegistration: Registration,
          subscriptionResponse: CreateEclSubscriptionResponse
        ) =>
          reset(mockNrsService)
          reset(mockAppConfig)

          when(mockAppConfig.amendRegistrationNrsEnabled)
            .thenReturn(true)

          val html         = "<html><head></head><body></body></html>"
          val registration = aRegistration.copy(
            registrationType = Some(Amendment),
            entityType = Some(Charity),
            base64EncodedFields = Some(Base64EncodedFields(None, Some(Base64.getEncoder.encodeToString(html.getBytes))))
          )

          val registrationAdditionalInfo =
            RegistrationAdditionalInfo.empty(registration.internalId)

          when(mockRegistrationService.getRegistration(any())(any()))
            .thenReturn(EitherT.rightT(registration))

          when(mockRegistrationAdditionalInfoService.get(ArgumentMatchers.eq(registration.internalId))(any()))
            .thenReturn(EitherT.rightT[Future, DataRetrievalError](registrationAdditionalInfo))

          when(mockRegistrationValidationService.validateRegistration(any()))
            .thenReturn(EitherT.rightT(registration))

          when(mockDmsService.submitToDms(any(), any(), any())(any()))
            .thenReturn(EitherT.rightT(subscriptionResponse.success))

          val result: Future[Result] =
            controller.submitRegistration(registration.internalId)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR

          verify(mockNrsService, times(0)).submitToNrs(
            any(),
            any(),
            any()
          )(any(), any())
      }
    }

    "return 404 NOT_FOUND when there is no registration data to submit" in forAll { registration: Registration =>
      when(mockRegistrationService.getRegistration(any())(any()))
        .thenReturn(EitherT.leftT(RegistrationError.NotFound(registration.internalId)))

      val result: Future[Result] =
        controller.submitRegistration(registration.internalId)(fakeRequest)

      status(result) shouldBe NOT_FOUND
    }
  }
}
