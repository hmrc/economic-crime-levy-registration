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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository
import uk.gov.hmrc.economiccrimelevyregistration.testonly.connectors.{EclStubsConnector, TestOnlyEnrolmentStoreProxyConnector, TestOnlyTaxEnrolmentsConnector}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class TestOnlyController @Inject() (
  cc: ControllerComponents,
  registrationRepository: RegistrationRepository,
  testOnlyTaxEnrolmentsConnector: TestOnlyTaxEnrolmentsConnector,
  testOnlyEnrolmentStoreProxyConnector: TestOnlyEnrolmentStoreProxyConnector,
  authorise: AuthorisedAction,
  eclStubsConnector: EclStubsConnector
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
          s"Enrolment ${EclEnrolment.ServiceName} successfully de-allocated from group ID $groupId with ECL reference $eclReference. " +
            s"The enrolment has also been de-assigned from all users within the group."
        )
      )
  }

  def deEnrolStubEclReferences(): Action[AnyContent] = Action.async { implicit request =>
    eclStubsConnector.getStubEclReferences
      .flatMap { references =>
        Future.traverse(references) { reference =>
          testOnlyEnrolmentStoreProxyConnector
            .getAllocatedPrincipalGroupIds(reference)
            .map(_.map { enrolmentGroupIdResponse =>
              Future.traverse(enrolmentGroupIdResponse.principalGroupIds)(groupId =>
                testOnlyEnrolmentStoreProxyConnector.deEnrol(groupId, reference).map {
                  case Right(_) =>
                    s"Enrolment ${EclEnrolment.ServiceName} successfully de-allocated from group ID $groupId with ECL reference $reference."
                  case Left(e)  =>
                    s"Failed to de-allocate enrolment ${EclEnrolment.ServiceName} from group ID $groupId with ECL reference $reference. Error: ${e.getMessage()}"
                }
              )
            })
        }
      }
      .map(_.flatMap(_.toList))
      .flatMap(Future.sequence(_))
      .map(_.flatten)
      .map(l => Ok(Json.toJson(l)))
  }
}
