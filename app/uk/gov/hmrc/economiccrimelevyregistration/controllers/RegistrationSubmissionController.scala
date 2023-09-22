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
import uk.gov.hmrc.economiccrimelevyregistration.services.{AuditService, DmsService, NrsService, RegistrationValidationService, SubscriptionService}
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
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submitRegistration(id: String) = authorise.async { implicit request =>
    registrationRepository.get(id).flatMap {
      case Some(registration) =>
        registrationValidationService.validateRegistration(registration) match {
          case Valid(Left(eclSubscription)) =>
            subscriptionService.subscribeToEcl(eclSubscription, registration).map { response =>
              nrsService.submitToNrs(
                registration.base64EncodedFields.flatMap(_.nrsSubmissionHtml),
                response.success.eclReference
              )
              Ok(Json.toJson(response.success))
            }
          case Valid(Right(registration))   =>
            dmsService.submitToDms(registration.base64EncodedFields.flatMap(_.dmsSubmissionHtml), Instant.now()).map {
              case Right(response) =>
                nrsService.submitToNrs(
                  registration.base64EncodedFields.flatMap(_.nrsSubmissionHtml),
                  response.eclReference
                )
                auditService
                  .successfulSubscriptionAndEnrolment(
                    registration,
                    response.eclReference
                  )
                Ok(Json.toJson(response))
              case Left(e)         =>
                logger.error(
                  s"Failed to submit PDF to DMS: ${e.getMessage()}"
                )
                InternalServerError("Could not send PDF to DMS queue")
            }
          case Invalid(e)                   =>
            logger.error(
              s"Invalid registration: ${e.toList.mkString(",")}"
            )
            Future.successful(InternalServerError(Json.toJson(DataValidationErrors(e.toList))))
        }
      case None               => Future.successful(NotFound)
    }
  }
}
