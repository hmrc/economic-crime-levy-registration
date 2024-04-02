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

import play.api.http.Status._
import play.api.libs.json._

sealed abstract class ErrorCode(val code: String, val statusCode: Int) extends Product with Serializable

object ErrorCode {
  case object BadGateway extends ErrorCode("BAD_GATEWAY", BAD_GATEWAY)

  case object BadRequest extends ErrorCode("BAD_REQUEST", BAD_REQUEST)

  case object InternalServerError extends ErrorCode("INTERNAL_SERVER_ERROR", INTERNAL_SERVER_ERROR)

  case object NotFound extends ErrorCode("NOT_FOUND", NOT_FOUND)

  case object Unauthorized extends ErrorCode("UNAUTHORIZED", UNAUTHORIZED)

  private lazy val errorCodes: Seq[ErrorCode] = Seq(
    BadGateway,
    BadRequest,
    InternalServerError,
    NotFound,
    Unauthorized
  )

  implicit val errorCodeWrites: Writes[ErrorCode] = Writes { errorCode =>
    JsString(errorCode.code)
  }

  implicit val errorCodeReads: Reads[ErrorCode] = Reads { errorCode =>
    errorCodes
      .find(value => value.code == errorCode.asInstanceOf[JsString].value)
      .map(errorCode => JsSuccess(errorCode))
      .getOrElse(JsError())
  }

}
