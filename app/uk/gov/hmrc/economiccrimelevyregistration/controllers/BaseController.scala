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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Status}
import uk.gov.hmrc.economiccrimelevyregistration.models.{RegistrationAdditionalInfo, SessionData}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.ResponseError

import scala.concurrent.{ExecutionContext, Future}

trait BaseController {

  def checkOptionalValueExists[T](value: Option[T]): EitherT[Future, ResponseError, T] = EitherT(
    Future.successful(
      value match {
        case Some(value) => Right(value)
        case None        => Left(ResponseError.internalServiceError())
      }
    )
  )

  implicit class ResponseHandler[R](value: EitherT[Future, ResponseError, R]) {

    def convertToResult(responseCode: Int)(implicit c: Converter[R], ec: ExecutionContext): Future[Result] =
      value.fold(
        err => Status(err.code.statusCode)(Json.toJson(err)),
        response => c.getResponse(response, responseCode)
      )
  }

  trait Converter[R] {
    def getResponse(response: R, responseCode: Int): Result
  }

  implicit val unitResponse: Converter[Unit] =
    new Converter[Unit] {
      override def getResponse(response: Unit, responseCode: Int): Status = Status(responseCode)
    }

  implicit val registrationAdditionalInfoResponse: Converter[RegistrationAdditionalInfo] =
    new Converter[RegistrationAdditionalInfo] {
      override def getResponse(response: RegistrationAdditionalInfo, responseCode: Int): Result = Status(responseCode)(
        Json.toJson(response)
      )
    }

  implicit val sessionDataResponse: Converter[SessionData] =
    new Converter[SessionData] {
      override def getResponse(response: SessionData, responseCode: Int): Result =
        Status(responseCode)(Json.toJson(response))
    }

}
