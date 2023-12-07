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

import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EclSubscription
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.services.{RegistrationAdditionalInfoService, RegistrationService, RegistrationValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class RegistrationValidationController @Inject() (
  cc: ControllerComponents,
  registrationService: RegistrationService,
  authorise: AuthorisedAction,
  registrationValidationService: RegistrationValidationService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging
    with BaseController
    with ErrorHandler {

  def checkForValidationErrors(id: String): Action[AnyContent] = authorise.async { _ =>
    (for {
      registration   <- registrationService.getRegistration(id).asResponseError
      additionalInfo <- registrationAdditionalInfoService.get(registration.internalId).asResponseError
      result          = resolveRegistrationValidationExecutionPath(registration, additionalInfo)
    } yield result).convertToResult(OK)
  }

  private def resolveRegistrationValidationExecutionPath(
    registration: Registration,
    additionalInfo: RegistrationAdditionalInfo
  ): Boolean                                                                                                           =
    if (registration.isRegistration()) {
      checkForErrorsInRegistrationValidation(registrationValidationService.validateRegistration(registration))
    } else {
      checkForErrorsInSubscriptionValidation(
        registrationValidationService.validateSubscription(registration, additionalInfo)
      )
    }
  private def checkForErrorsInRegistrationValidation(registration: Either[DataValidationError, Registration]): Boolean =
    registration match {
      case Left(_)  => true
      case Right(_) => false
    }

  private def checkForErrorsInSubscriptionValidation(
    eclSubscription: Either[DataValidationError, EclSubscription]
  ): Boolean =
    eclSubscription match {
      case Left(_)  => true
      case Right(_) => false
    }
}
