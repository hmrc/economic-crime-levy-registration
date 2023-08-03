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

import cats.implicits.catsSyntaxValidatedId
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary
import play.api.Play.materializer
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{AUTHORIZATION, POST, route, status, stubControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Other
import uk.gov.hmrc.economiccrimelevyregistration.models.dms.DmsNotification
import uk.gov.hmrc.economiccrimelevyregistration.models.dms.SubmissionItemStatus.Processed
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError.DataInvalid
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataValidationError, DataValidationErrors}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, CreateEclSubscriptionResponsePayload, EclSubscription}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.economiccrimelevyregistration.services.{DmsService, NrsService, RegistrationValidationService, SubscriptionService}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, Retrieval}

import java.util.Base64
import scala.concurrent.Future

class RegistrationSubmissionControllerSpec extends SpecBase {

  val mockRegistrationValidationService: RegistrationValidationService = mock[RegistrationValidationService]
  val mockSubscriptionServiceService: SubscriptionService              = mock[SubscriptionService]
  val mockRegistrationRepository: RegistrationRepository               = mock[RegistrationRepository]
  val mockNrsService: NrsService                                       = mock[NrsService]
  val mockDmsService: DmsService                                       = mock[DmsService]
  val mockStubBehaviour: StubBehaviour = mock[StubBehaviour]
  val stubBackendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

  val controller = new RegistrationSubmissionController(
    cc,
    mockRegistrationRepository,
    fakeAuthorisedAction,
    mockRegistrationValidationService,
    mockSubscriptionServiceService,
    stubBackendAuthComponents,
    mockNrsService,
    mockDmsService
  )

  "submitRegistration" should {
    "return 200 OK with a subscription reference number in the JSON response body when the registration data is valid for 'Normal' entities" in forAll (
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[EntityType].retryUntil(_ != Other),
      Arbitrary.arbitrary[EclSubscription],
      Arbitrary.arbitrary[CreateEclSubscriptionResponse]
    )
    {
      (
        aRegistration: Registration,
        entityType: EntityType,
        eclSubscription: EclSubscription,
        subscriptionResponse: CreateEclSubscriptionResponse
      ) =>
        val registration = aRegistration.copy(
          entityType = Some(entityType)
        )

        when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(Some(registration)))

        when(mockRegistrationValidationService.validateRegistration(any())).thenReturn(Left(eclSubscription).validNel)

        when(
          mockSubscriptionServiceService
            .subscribeToEcl(ArgumentMatchers.eq(eclSubscription), ArgumentMatchers.eq(registration))(any())
        )
          .thenReturn(Future.successful(subscriptionResponse))

        val result: Future[Result] =
          controller.submitRegistration(registration.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)

        verify(mockNrsService, times(1)).submitToNrs(any(), any())(any(), any())

        reset(mockNrsService)
    }

    "return 200 OK with a subscription reference number in the JSON response body when the registration data is valid for 'Other' entities" in forAll (
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[CreateEclSubscriptionResponse]
    )
    {
      (
        aRegistration: Registration,
        subscriptionResponse: CreateEclSubscriptionResponse
      ) =>
        val html = "<html><head></head><body></body></html>"
        val registration = aRegistration.copy(
          entityType = Some(Other),
          base64EncodedDmsSubmissionHtml = Some(Base64.getEncoder.encodeToString(html.getBytes))
        )

        when(mockRegistrationRepository.get(any()))
          .thenReturn(Future.successful(Some(registration)))

        when(mockRegistrationValidationService.validateRegistration(any()))
          .thenReturn(Right(registration).validNel)

        when(mockDmsService.submitToDms(any(), any())(any()))
          .thenReturn(Future.successful(subscriptionResponse.success))

        val result: Future[Result] =
          controller.submitRegistration(registration.internalId)(fakeRequest)

        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)
    }

    "return 500 INTERNAL_SERVER_ERROR with validation errors in the JSON response body when the registration data is invalid" in forAll (
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[EntityType].retryUntil(_ != Other)
    )
    {
      (
        aRegistration: Registration,
        entityType: EntityType
      ) =>
        val registration = aRegistration.copy(
          entityType = Some(entityType)
        )

        when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(Some(registration)))

        when(mockRegistrationValidationService.validateRegistration(any()))
          .thenReturn(DataValidationError(DataInvalid, "Invalid data").invalidNel)

        val result: Future[Result] =
          controller.submitRegistration(registration.internalId)(fakeRequest)

        status(result)        shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.toJson(
          DataValidationErrors(Seq(DataValidationError(DataInvalid, "Invalid data")))
        )
    }

    "return 404 NOT_FOUND when there is no registration data to submit" in forAll { registration: Registration =>
      when(mockRegistrationRepository.get(any())).thenReturn(Future.successful(None))

      val result: Future[Result] =
        controller.submitRegistration(registration.internalId)(fakeRequest)

      status(result) shouldBe NOT_FOUND
    }
  }

  "dmsCallback" should {
    "process notifications from DMS" in forAll { dmsNotification: DmsNotification =>
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest(POST, routes.RegistrationSubmissionController.dmsCallback.url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withBody(Json.toJson(dmsNotification))

      val result = controller.dmsCallback()(request)
      status(result) shouldBe OK
    }
  }

}
