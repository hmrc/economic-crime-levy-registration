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
      registrationRepository.upsert(registration).map(result => Right(result)).recover { case e =>
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
      registrationRepository.clear(id).map(Right(_)).recover { case e =>
        Left(RegistrationError.InternalUnexpectedError(e.getMessage, Some(e)))
      }
    }
}
