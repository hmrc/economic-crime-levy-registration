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

package uk.gov.hmrc.economiccrimelevyregistration.models.errors

import play.api.libs.Files.logger
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import play.api.libs.json.__
import play.api.libs.functional.syntax.toFunctionalBuilderOps

sealed abstract class ResponseError extends Product with Serializable {
  def message: String
  def code: ErrorCode
}

object ResponseError {

  private val codeFieldName    = "code"
  private val messageFieldName = "message"

  def badRequestError(message: String): ResponseError =
    StandardError(message, ErrorCode.BadRequest)

  def notFoundError(message: String): ResponseError =
    StandardError(message, ErrorCode.NotFound)

  def badGateway(message: String, code: Int): ResponseError =
    BadGateway(message, ErrorCode.BadGateway, code)

  def internalServiceError(
    message: String = "Internal server error",
    code: ErrorCode = ErrorCode.InternalServerError,
    cause: Option[Throwable] = None
  ): ResponseError = {
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

    InternalServiceError("Internal server error", code, cause)
  }

  implicit val errorWrites: OWrites[ResponseError] =
    (
      (__ \ messageFieldName).write[String] and
        (__ \ codeFieldName).write[ErrorCode]
    )(unlift(ResponseError.unapply))

  implicit val standardErrorReads: Reads[StandardError] =
    (
      (__ \ messageFieldName).read[String] and
        (__ \ codeFieldName).read[ErrorCode]
    )(StandardError.apply _)

  def unapply(error: ResponseError): Option[(String, ErrorCode)] = Some((error.message, error.code))
}

case class StandardError(message: String, code: ErrorCode) extends ResponseError

case class BadGateway(
  message: String = "Internal server error",
  code: ErrorCode = ErrorCode.BadGateway,
  responseCode: Int
) extends ResponseError

object BadGateway {}
case class InternalServiceError(
  message: String = "Internal server error",
  code: ErrorCode = ErrorCode.InternalServerError,
  cause: Option[Throwable] = None
) extends ResponseError

object InternalServiceError {}
