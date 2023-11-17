package uk.gov.hmrc.economiccrimelevyregistration.models.errors

trait RegistrationError

object RegistrationError {
  case class NotFound(id: String) extends RegistrationError

  case class InternalUnexpectedError(message: String, cause: Option[Throwable]) extends RegistrationError
}
