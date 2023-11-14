package uk.gov.hmrc.economiccrimelevyregistration.models.errors

sealed trait NrsSubmissionError

object NrsSubmissionError {
  case class InternalUnexpectedError(message: String, cause: Option[Throwable]) extends NrsSubmissionError

  case class BadGateway(reason: String, code: Int) extends NrsSubmissionError
}
