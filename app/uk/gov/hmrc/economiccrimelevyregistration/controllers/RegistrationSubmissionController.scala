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

import cats.data.Validated.{Invalid, Valid}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.economiccrimelevyregistration.services.{AuditService, DmsService, NrsService, RegistrationAdditionalInfoService, RegistrationValidationService, SubscriptionService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationSubmissionController @Inject() (
  cc: ControllerComponents,
  registrationRepository: RegistrationRepository,
  authorise: AuthorisedAction,
  registrationValidationService: RegistrationValidationService,
  subscriptionService: SubscriptionService,
  nrsService: NrsService,
  dmsService: DmsService,
  auditService: AuditService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submitRegistration(id: String) = authorise.async { implicit request =>
    registrationRepository.get(id).flatMap {
      case Some(registration) =>
        registrationAdditionalInfoService
          .get(registration.internalId)
          .foldF(
            error => {
              logger.error(
                s"Failed to find additional information for amendment with internal id: ${registration.internalId}"
              )
              Future.successful(InternalServerError("Failed to find additional information for amendment"))
            },
            additionalInfo =>
              registrationValidationService.validateRegistration(registration, additionalInfo) match {
                case Valid(Left(eclSubscription)) =>
                  subscriptionService.subscribeToEcl(eclSubscription, registration).map { response =>
                    nrsService.submitToNrs(
                      registration.base64EncodedFields.flatMap(_.nrsSubmissionHtml),
                      response.success.eclReference,
                      appConfig.eclFirstTimeRegistrationNotableEvent
                    )
                    Ok(Json.toJson(response.success))
                  }

                case Valid(Right(registration)) if registration.registrationType.contains(Amendment) =>
                  additionalInfo.eclReference match {
                    case Some(eclRef) =>
                      dmsService
                        .submitToDms(registration.base64EncodedFields.flatMap(_.dmsSubmissionHtml), Instant.now())
                        .flatMap {
                          case Right(response) =>
                            if (appConfig.amendRegistrationNrsEnabled) {
                              nrsService.submitToNrs(
                                registration.base64EncodedFields.flatMap(_.nrsSubmissionHtml),
                                eclRef,
                                appConfig.eclAmendRegistrationNotableEvent
                              )
                            }
                            auditService
                              .successfulSubscriptionAndEnrolment(
                                registration,
                                eclRef
                              )
                            Future.successful(Ok(Json.toJson(response)))
                          case Left(e)         =>
                            logger.error(
                              s"Failed to submit PDF to DMS: ${e.getMessage()}"
                            )
                            Future.successful(InternalServerError("Could not send PDF to DMS queue"))
                        }
                    case None         =>
                      logger.error(
                        s"Expected eclReference to be present in additional information for amendment with internal id: ${registration.internalId}"
                      )

                      Future.successful(
                        InternalServerError("Expected eclReference to be present in additional information")
                      )
                  }

                case Valid(Right(registration)) =>
                  dmsService
                    .submitToDms(registration.base64EncodedFields.flatMap(_.dmsSubmissionHtml), Instant.now())
                    .flatMap {
                      case Right(response) =>
                        nrsService.submitToNrs(
                          registration.base64EncodedFields.flatMap(_.nrsSubmissionHtml),
                          response.eclReference,
                          appConfig.eclFirstTimeRegistrationNotableEvent
                        )

                        auditService
                          .successfulSubscriptionAndEnrolment(
                            registration,
                            response.eclReference
                          )
                        Future.successful(Ok(Json.toJson(response)))
                      case Left(e)         =>
                        logger.error(
                          s"Failed to submit PDF to DMS: ${e.getMessage()}"
                        )
                        Future.successful(InternalServerError("Could not send PDF to DMS queue"))
                    }
                case Invalid(e)                 =>
                  logger.error(
                    s"Invalid registration: ${e.toList.mkString(",")}"
                  )
                  Future.successful(InternalServerError(Json.toJson(DataValidationErrors(e.toList))))
              }
          )
      case None               => Future.successful(NotFound)
    }
  }

}
