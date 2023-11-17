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
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{BadGateway, DataRetrievalError, DmsSubmissionError, InternalServiceError, KnownFactsError, RegistrationError, ResponseError}

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
                   |Trace: ${ex.getStackTrace.mkString(System.lineSeparator())}
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

  trait Converter[E] {
    def convert(error: E): ResponseError
  }

  implicit val dataRetrievalErrorConverter: Converter[DataRetrievalError] =
    new Converter[DataRetrievalError] {
      override def convert(error: DataRetrievalError): ResponseError = error match {
        case DataRetrievalError.NotFound(id)                            => ResponseError.notFoundError(s"Unable to find record with id: $id")
        case DataRetrievalError.InternalUnexpectedError(message, cause) =>
          ResponseError.internalServiceError(message = message, cause = cause)
      }
    }

  implicit val knownFactsErrorConverter: Converter[KnownFactsError] =
    new Converter[KnownFactsError] {
      override def convert(error: KnownFactsError): ResponseError = error match {
        case KnownFactsError.UpsertKnownFactsError(message) => ResponseError.badRequestError(message)
        case KnownFactsError.NotFound(message)              => ResponseError.badRequestError(message)
      }
    }

  implicit val dmsSubmissionErrorConverter: Converter[DmsSubmissionError] =
    new Converter[DmsSubmissionError] {
      override def convert(error: DmsSubmissionError): ResponseError = error match {
        case DmsSubmissionError.InternalUnexpectedError(message, cause) =>
          ResponseError.internalServiceError(message = message, cause = cause)
        case DmsSubmissionError.BadGateway(reason, code)                => ResponseError.badGateway(reason, code)
      }
    }

  implicit val registrationErrorConverter: Converter[RegistrationError] =
    new Converter[RegistrationError] {
      override def convert(error: RegistrationError): ResponseError = error match {
        case RegistrationError.InternalUnexpectedError(message, cause) =>
          ResponseError.internalServiceError(message = message, cause = cause)
        case RegistrationError.NotFound(id) => ResponseError.notFoundError(s"Unable to find record with id: $id")
      }
    }

}
