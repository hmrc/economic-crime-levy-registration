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

import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyregistration.models.errors._

import scala.concurrent.{ExecutionContext, Future}

trait ErrorHandler extends Logging {

  implicit class ErrorConvertor[E, R](value: EitherT[Future, E, R]) {
    def asResponseError(implicit c: Converter[E], ec: ExecutionContext): EitherT[Future, ResponseError, R] =
      value.leftMap(c.convert).leftSemiflatTap {
        case InternalServiceError(message, _, cause) =>
          val causeText = cause
            .map { ex =>
              s"""
                   |Message: ${ex.getMessage}
                   |""".stripMargin
            }
            .getOrElse("No exception is available")
          logger.error(s"""Internal Server Error: $message
               |
               |$causeText""".stripMargin)
          Future.successful(())
        case BadGateway(message, _, responseCode)    =>
          val causeText = s"""
                 |Message: $message
                 |Upstream status code: $responseCode
                 |""".stripMargin

          logger.error(s"""Bad gateway: $message
               |
               |$causeText""".stripMargin)
          Future.successful(())
        case _                                       => Future.successful(())
      }
  }
  def valueOrError[T](value: Option[T], valueType: String): EitherT[Future, ResponseError, T] =
    EitherT {
      Future.successful(value.map(Right(_)).getOrElse(Left(ResponseError.internalServiceError(s"Missing $valueType"))))
    }

  trait Converter[E] {
    def convert(error: E): ResponseError
  }

  implicit val dataRetrievalErrorConverter: Converter[DataRetrievalError] = {
    case DataRetrievalError.NotFound(id)                      => ResponseError.notFoundError(s"Unable to find record with id: $id")
    case DataRetrievalError.InternalUnexpectedError(_, cause) =>
      ResponseError.internalServiceError(cause = cause)
  }

  implicit val knownFactsErrorConverter: Converter[KnownFactsError] = {
    case KnownFactsError.UpsertKnownFactsError(message) => ResponseError.badRequestError(message)
    case KnownFactsError.NotFound(message)              => ResponseError.badRequestError(message)
  }

  implicit val dmsSubmissionErrorConverter: Converter[DmsSubmissionError] = {
    case DmsSubmissionError.InternalUnexpectedError(cause) =>
      ResponseError.internalServiceError(cause = cause)
    case DmsSubmissionError.BadGateway(reason, code)       => ResponseError.badGateway(reason, code)
  }

  implicit val registrationErrorConverter: Converter[RegistrationError] = {
    case RegistrationError.InternalUnexpectedError(cause) =>
      ResponseError.internalServiceError(cause = cause)
    case RegistrationError.NotFound(id)                   => ResponseError.notFoundError(s"Unable to find record with id: $id")
  }

  implicit val subscriptionSubmissionErrorConverter: Converter[SubscriptionSubmissionError] = {
    case SubscriptionSubmissionError.InternalUnexpectedError(_, cause) =>
      ResponseError.internalServiceError(cause = cause)
    case SubscriptionSubmissionError.BadGateway(reason, code)          => ResponseError.badGateway(reason, code)
  }

  implicit val dataValidationErrorConverter: Converter[DataValidationError] = {
    case DataValidationError.DataInvalid(message)           => ResponseError.badRequestError(message)
    case DataValidationError.SchemaValidationError(message) =>
      ResponseError.badRequestError(message)
    case DataValidationError.DataMissing(message)           => ResponseError.badRequestError(message)
  }

  implicit val nrsSubmissionErrorConverter: Converter[NrsSubmissionError] = {
    case NrsSubmissionError.InternalUnexpectedError(cause) =>
      ResponseError.internalServiceError(cause = cause)
    case NrsSubmissionError.BadGateway(reason, code)       => ResponseError.badGateway(reason, code)
    case NrsSubmissionError.EclReferenceNotFound(message)  => ResponseError.internalServiceError(message)
  }
}
