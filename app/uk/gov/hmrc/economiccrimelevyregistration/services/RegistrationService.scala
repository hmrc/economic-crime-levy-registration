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

package uk.gov.hmrc.economiccrimelevyregistration.services

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.RegistrationError
import uk.gov.hmrc.economiccrimelevyregistration.repositories.RegistrationRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject() (
  registrationRepository: RegistrationRepository
) {
  def upsertRegistration(
    registration: Registration
  )(implicit ec: ExecutionContext): EitherT[Future, RegistrationError, Registration] =
    EitherT {
      registrationRepository.upsert(registration).map(_ => Right(registration)).recover { case e =>
        Left(RegistrationError.InternalUnexpectedError(e.getMessage, Some(e.getCause)))
      }
    }

  def getRegistration(id: String)(implicit ec: ExecutionContext): EitherT[Future, RegistrationError, Registration] =
    EitherT {
      registrationRepository.get(id).map {
        case Some(value) => Right(value)
        case None        => Left(RegistrationError.NotFound(id))
      }
    }

  def deleteRegistration(id: String)(implicit ec: ExecutionContext): EitherT[Future, RegistrationError, Unit] =
    EitherT {
      registrationRepository.clear(id).map(response => Right(())).recover { case e =>
        Left(RegistrationError.InternalUnexpectedError(e.getMessage, Some(e)))
      }
    }
}
