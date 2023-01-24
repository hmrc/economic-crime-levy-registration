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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.controllers

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.economiccrimelevyregistration.testonly.connectors.TestOnlyTaxEnrolmentsConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class TestOnlyController @Inject() (
  cc: ControllerComponents,
  registrationRepository: RegistrationRepository,
  testOnlyTaxEnrolmentsConnector: TestOnlyTaxEnrolmentsConnector,
  authorise: AuthorisedAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def clearAllData: Action[AnyContent] = Action.async { _ =>
    registrationRepository.collection.drop().toFuture().map(_ => Ok("All data cleared"))
  }

  def clearCurrentData: Action[AnyContent] = authorise.async { implicit request =>
    registrationRepository.clear(request.internalId).map(_ => Ok("Current user data cleared"))
  }

  def deEnrol(groupId: String, eclReference: String): Action[AnyContent] = authorise.async { implicit request =>
    testOnlyTaxEnrolmentsConnector
      .deEnrol(groupId, eclReference)
      .map(_ =>
        Ok(
          s"Enrolment ${EclEnrolment.ServiceName} successfully de-allocated from group ID $groupId with ECL reference $eclReference. The enrolment has also been de-assigned from all users within the group."
        )
      )
  }

}
