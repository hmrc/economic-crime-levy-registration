/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class RegistrationController @Inject() (
  cc: ControllerComponents,
  registrationRepository: RegistrationRepository,
  authorise: AuthorisedAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def createRegistration(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[Registration] { registration =>
      registrationRepository.upsert(registration).map(_ => Ok(Json.toJson(registration)))
    }
  }

  def getRegistration(id: String): Action[AnyContent] = authorise.async { implicit request =>
    registrationRepository.get(id).map {
      case Some(registration) => Ok(Json.toJson(registration))
      case None               => NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "Registration not found")))
    }
  }

  def updateRegistration(id: String): Action[AnyContent] = authorise.async { implicit request =>
    ???
  }

  def deleteRegistration(id: String): Action[AnyContent] = authorise.async { implicit request =>
    ???
  }

}
