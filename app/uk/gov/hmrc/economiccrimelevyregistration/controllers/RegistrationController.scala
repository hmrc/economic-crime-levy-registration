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

import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.services.RegistrationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class RegistrationController @Inject() (
  cc: ControllerComponents,
  registrationService: RegistrationService,
  authorise: AuthorisedAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController
    with ErrorHandler {

  def upsertRegistration: Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[Registration] { registration =>
      (for {
        updatedRegistration <- registrationService.upsertRegistration(registration).asResponseError
      } yield updatedRegistration).convertToResult(OK)
    }
  }

  def getRegistration(id: String): Action[AnyContent] = authorise.async { _ =>
    (for {
      registration <- registrationService.getRegistration(id).asResponseError
    } yield registration).convertToResult(OK)
  }

  def deleteRegistration(id: String): Action[AnyContent] = authorise.async { _ =>
    (for {
      _ <- registrationService.deleteRegistration(id).asResponseError
    } yield ()).convertToResult(OK)
  }

}
