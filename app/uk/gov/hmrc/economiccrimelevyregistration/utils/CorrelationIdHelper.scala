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

package uk.gov.hmrc.economiccrimelevyregistration.utils

import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.util.UUID

object CorrelationIdHelper {

  def headerCarrierWithCorrelationId(request: Request[_]): HeaderCarrier = {
    val hcFromRequest = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val newHc: HeaderCarrier = hcFromRequest.headers(Seq(HttpConstants.HEADER_X_CORRELATION_ID)) match {
      case Nil => hcFromRequest.withExtraHeaders((HttpConstants.HEADER_X_CORRELATION_ID, UUID.randomUUID().toString))
      case _   => hcFromRequest
    }

    request.headers
      .get(HeaderNames.authorisation)
      .map(auth => newHc.withExtraHeaders((HeaderNames.authorisation, auth)))
      .getOrElse(newHc)

  }
}
