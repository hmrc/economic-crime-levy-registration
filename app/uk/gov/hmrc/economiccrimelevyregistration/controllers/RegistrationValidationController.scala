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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.economiccrimelevyregistration.services.{RegistrationAdditionalInfoService, RegistrationValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class RegistrationValidationController @Inject() (
  cc: ControllerComponents,
  registrationRepository: RegistrationRepository,
  authorise: AuthorisedAction,
  registrationValidationService: RegistrationValidationService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getValidationErrors(id: String): Action[AnyContent] = authorise.async { _ =>
    registrationRepository.get(id).flatMap {
      case Some(registration) =>
        registrationAdditionalInfoService
          .get(registration.internalId)
          .fold(
            error => {
              logger.error(
                s"Failed to find additional information for amendment with internal id: ${registration.internalId}"
              )
              NotFound
            },
            additionalInfo =>
              registrationValidationService.validateRegistration(registration, additionalInfo) match {
                case Valid(_)   => NoContent
                case Invalid(e) => Ok(Json.toJson(DataValidationErrors(e.toList)))
              }
          )
      case None               => Future.successful(NotFound)
    }
  }

}
