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

package uk.gov.hmrc.economiccrimelevyregistration.services.deregister

import cats.data.EitherT
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.DeregistrationRequestedAuditEvent
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{AuditError, RegistrationError}
import uk.gov.hmrc.economiccrimelevyregistration.repositories.deregister.DeregistrationRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class DeregistrationService @Inject() (
  deregistrationRepository: DeregistrationRepository,
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  def deleteDeregistration(id: String)(implicit ec: ExecutionContext): EitherT[Future, RegistrationError, Unit] =
    EitherT {
      deregistrationRepository.deleteRecord(id).map(response => Right(())).recover { case e =>
        Left(RegistrationError.InternalUnexpectedError(Some(e)))
      }
    }

  def getDeregistration(id: String)(implicit ec: ExecutionContext): EitherT[Future, RegistrationError, Deregistration] =
    EitherT {
      deregistrationRepository
        .get(id)
        .map {
          case Some(value) => Right(value)
          case None        => Left(RegistrationError.NotFound(id))
        }
        .recover { case e =>
          Left(RegistrationError.InternalUnexpectedError(Some(e)))
        }
    }

  def sendDeregistrationRequestedAuditEvent(
    deregistrationData: Deregistration
  )(implicit hc: HeaderCarrier): EitherT[Future, AuditError, Unit] =
    EitherT {
      val auditEvent = DeregistrationRequestedAuditEvent(
        deregistrationData.copy(dmsSubmissionHtml = None)
      ).extendedDataEvent

      auditConnector
        .sendExtendedEvent(auditEvent)(hc, ec)
        .map {
          case AuditResult.Success            => Right(())
          case AuditResult.Failure(reason, _) =>
            Left(AuditError.BadGateway(reason = s"Deregistration Requested Audit Failed - $reason", code = BAD_GATEWAY))
          case AuditResult.Disabled           =>
            Left(AuditError.BadGateway(reason = "Audit is disabled for the audit connector", code = BAD_GATEWAY))
        }
        .recover { case NonFatal(e) =>
          Left(AuditError.InternalUnexpectedError(Some(e)))
        }
    }

  def upsertDeregistration(
    deregistration: Deregistration
  )(implicit ec: ExecutionContext): EitherT[Future, RegistrationError, Deregistration] =
    EitherT {
      deregistrationRepository.upsert(deregistration).map(_ => Right(deregistration)).recover { case e =>
        Left(RegistrationError.InternalUnexpectedError(Some(e)))
      }
    }
}
