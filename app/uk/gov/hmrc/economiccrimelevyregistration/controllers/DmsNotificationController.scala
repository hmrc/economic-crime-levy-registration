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
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.dms.{DmsNotification, SubmissionItemStatus}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.economiccrimelevyregistration.services.{DmsService, NrsService, RegistrationValidationService, SubscriptionService}
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsNotificationController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val predicate = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType(appConfig.appName),
      resourceLocation = ResourceLocation(routes.DmsNotificationController.dmsCallback().url)
    ),
    action = IAAction("WRITE")
  )

  private val authorised = auth.authorizedAction(predicate)

  def dmsCallback: Action[JsValue] = authorised(parse.json) { implicit request =>
    request.body.validate[DmsNotification] match {
      case JsSuccess(notification, _) =>
        if (notification.status == SubmissionItemStatus.Failed) {
          logger.error(
            s"DMS notification received for ${notification.id} failed with error: ${notification.failureReason
              .getOrElse("")}"
          )
        } else {
          logger.info(
            s"DMS notification received for ${notification.id} with status ${notification.status}"
          )
        }
        Ok
      case JsError(_)                 =>
        BadRequest
    }
  }
}
