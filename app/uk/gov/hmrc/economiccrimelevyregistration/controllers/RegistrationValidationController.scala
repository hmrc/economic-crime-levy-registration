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
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.EclSubscription
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.services.{RegistrationAdditionalInfoService, RegistrationService, RegistrationValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
    } yield (registration, additionalInfo)).foldF(
      error => Future.successful(Status(error.code.statusCode)(Json.toJson(error))),
      registrationAndInfo =>
        resolveRegistrationValidationExecutionPath(registrationAndInfo._1, registrationAndInfo._2).fold(
          error => Ok(Json.toJson(error.errorMessage)),
          _ => Ok(Json.toJson(None))
        )
    )
  }

  private def resolveRegistrationValidationExecutionPath(
    registration: Registration,
    additionalInfo: RegistrationAdditionalInfo
  ): EitherT[Future, DataValidationError, Unit] =
    if (registration.isRegistration) {
      registrationValidationService.validateRegistration(registration).map(_ => ())
    } else {
      registrationValidationService.validateSubscription(registration, additionalInfo).map(_ => ())
    }

}
