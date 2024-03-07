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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{NrsSubmissionError, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponsePayload
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.services._
import uk.gov.hmrc.economiccrimelevyregistration.utils.CorrelationIdHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationSubmissionController @Inject() (
  cc: ControllerComponents,
  registrationService: RegistrationService,
  authorise: AuthorisedAction,
  registrationValidationService: RegistrationValidationService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  nrsService: NrsService,
  dmsService: DmsService,
  auditService: AuditService,
  subscriptionService: SubscriptionService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController
    with ErrorHandler {

  def submitRegistration(id: String): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.headerCarrierWithCorrelationId(request)

    (for {
      registration   <- registrationService.getRegistration(id).asResponseError
      additionalInfo <- registrationAdditionalInfoService.get(registration.internalId).asResponseError
      response       <- if (registration.isRegistration) {
                          registerForEcl(registration, Some(additionalInfo))
                        } else {
                          subscribeToEcl(registration, additionalInfo)
                        }
    } yield response).convertToResult(OK)
  }

  private def subscribeToEcl(registration: Registration, additionalInfo: RegistrationAdditionalInfo)(implicit
    hc: HeaderCarrier,
    request: AuthorisedRequest[_]
  ): EitherT[Future, ResponseError, CreateEclSubscriptionResponsePayload] =
    for {
      sub      <- registrationValidationService
                    .validateSubscription(EclRegistrationModel(registration, Some(additionalInfo)))
                    .asResponseError
      response <- subscriptionService.subscribeToEcl(sub, registration, None).asResponseError
      _         = if (appConfig.nrsSubmissionEnabled) {
                    nrsService
                      .submitToNrs(
                        registration.base64EncodedFields.flatMap(_.nrsSubmissionHtml),
                        response.success.eclReference,
                        appConfig.eclFirstTimeRegistrationNotableEvent
                      )
                      .asResponseError
                  }
    } yield response.success

  private def registerForEcl(
    registration: Registration,
    registrationAdditionalInfo: Option[RegistrationAdditionalInfo]
  )(implicit
    hc: HeaderCarrier,
    request: AuthorisedRequest[_]
  ): EitherT[Future, ResponseError, CreateEclSubscriptionResponsePayload] =
    for {
      _                <- registrationValidationService
                            .validateRegistration(EclRegistrationModel(registration, registrationAdditionalInfo))
                            .asResponseError
      now               = Instant.now().truncatedTo(ChronoUnit.SECONDS)
      registrationType <- valueOrError(registration.registrationType, "Registration type")
      response         <- dmsService
                            .submitToDms(
                              registration.base64EncodedFields.flatMap(_.dmsSubmissionHtml),
                              now,
                              registrationType
                            )
                            .asResponseError
      additionalInfo   <-
        valueOrError(registrationAdditionalInfo, "Registration additional info")
      _                <- submitToNrsIfAmendment(registration, additionalInfo).asResponseError
      _                 = auditService.successfulSubscriptionAndEnrolment(
                            registration,
                            response.eclReference,
                            additionalInfo.liabilityYear
                          )
    } yield response

  private def submitToNrsIfAmendment(
    registration: Registration,
    additionalInfo: RegistrationAdditionalInfo
  )(implicit hc: HeaderCarrier, authorisedRequest: AuthorisedRequest[_]): EitherT[Future, NrsSubmissionError, Unit] =
    if (appConfig.amendRegistrationNrsEnabled && registration.registrationType.contains(Amendment)) {
      for {
        eclReference <- eclReferenceFromAdditionalInfoOrError(additionalInfo)
        response     <- nrsService
                          .submitToNrs(
                            registration.base64EncodedFields.flatMap(_.nrsSubmissionHtml),
                            eclReference,
                            appConfig.eclAmendRegistrationNotableEvent
                          )
                          .map(_ => ())
      } yield response
    } else {
      EitherT[Future, NrsSubmissionError, Unit](Future.successful(Right(())))
    }

  private def eclReferenceFromAdditionalInfoOrError(
    additionalInfo: RegistrationAdditionalInfo
  ): EitherT[Future, NrsSubmissionError.EclReferenceNotFound, String] =
    EitherT {
      Future.successful(
        additionalInfo.eclReference
          .map(Right(_))
          .getOrElse(
            Left(NrsSubmissionError.EclReferenceNotFound("Ecl Reference not found in RegistrationAdditionalInfo"))
          )
      )
    }
}
