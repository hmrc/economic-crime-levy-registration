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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{BaseController, ErrorHandler}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.DeRegistration
import uk.gov.hmrc.economiccrimelevyregistration.services._
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.utils.CorrelationIdHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeregistrationSubmissionController @Inject() (
  cc: ControllerComponents,
  deregistrationService: DeregistrationService,
  authorise: AuthorisedAction,
  dmsService: DmsService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController
    with ErrorHandler {
  def submitDeregistration(id: String): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.headerCarrierWithCorrelationId(request)

    (for {
      deregistration <- deregistrationService.getDeregistration(id).asResponseError
      now             = Instant.now().truncatedTo(ChronoUnit.SECONDS)
      _              <- dmsService
                          .submitToDms(deregistration.dmsSubmissionHtml, now, DeRegistration)
                          .asResponseError
      _               = deregistrationService.sendDeregistrationRequestedAuditEvent(deregistration)
    } yield ()).convertToResult(OK)
  }

}
