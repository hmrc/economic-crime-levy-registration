/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataValidationError, DmsSubmissionError, KnownFactsError, NrsSubmissionError, RegistrationError, ResponseError, SubscriptionSubmissionError}

class ErrorHandlerSpec extends SpecBase with ErrorHandler {

  "knownFactsErrorConverter" should {
    "return ResponseError.badRequestError when KnownFactsError.UpsertKnownFactsError is converted" in {
      val message = "Error message"

      val knownFactsError = KnownFactsError.UpsertKnownFactsError(message)

      val result = knownFactsErrorConverter.convert(knownFactsError)

      result shouldBe ResponseError.badRequestError(message)
    }
  }

  "dmsSubmissionErrorConverter" should {
    "return ResponseError.internalServiceError when DmsSubmissionError.InternalUnexpectedError is converted" in {
      val message   = "Error message"
      val exception = new Exception(message)

      val dmsSubmissionError = DmsSubmissionError.InternalUnexpectedError(Some(exception))

      val result = dmsSubmissionErrorConverter.convert(dmsSubmissionError)

      result shouldBe ResponseError.internalServiceError(cause = Some(exception))
    }
  }

  "registrationErrorConverter" should {
    "return ResponseError.internalServiceError when RegistrationError.InternalUnexpectedError is converted" in {
      val message   = "Error message"
      val exception = new Exception(message)

      val registrationError = RegistrationError.InternalUnexpectedError(Some(exception))

      val result = registrationErrorConverter.convert(registrationError)

      result shouldBe ResponseError.internalServiceError(cause = Some(exception))
    }
  }

  "subscriptionSubmissionErrorConverter" should {
    "return ResponseError.internalServiceError when SubscriptionSubmissionError.InternalUnexpectedError is converted" in {
      val message   = "Error message"
      val exception = new Exception(message)

      val registrationError = SubscriptionSubmissionError.InternalUnexpectedError(message, Some(exception))

      val result = subscriptionSubmissionErrorConverter.convert(registrationError)

      result shouldBe ResponseError.internalServiceError(cause = Some(exception))
    }

    "return ResponseError.badGateway when SubscriptionSubmissionError.BadGateway is converted" in {
      val message = "Error message"

      val registrationError = SubscriptionSubmissionError.BadGateway(message, BAD_GATEWAY)

      val result = subscriptionSubmissionErrorConverter.convert(registrationError)

      result shouldBe ResponseError.badGateway(message, BAD_GATEWAY)
    }
  }

  "dataValidationErrorConverter" should {
    "return ResponseError.badRequestError when DataValidationError.SchemaValidationError is converted" in {
      val message = "Error message"

      val dataValidationError = DataValidationError.SchemaValidationError(message)

      val result = dataValidationErrorConverter.convert(dataValidationError)

      result shouldBe ResponseError.badRequestError(message)
    }

    "return ResponseError.badRequestError when DataValidationError.DataMissing is converted" in {
      val message = "Error message"

      val dataValidationError = DataValidationError.DataMissing(message)

      val result = dataValidationErrorConverter.convert(dataValidationError)

      result shouldBe ResponseError.badRequestError(message)
    }
  }

  "nrsSubmissionErrorConverter" should {
    "return ResponseError.badRequestError when NrsSubmissionError.InternalUnexpectedError is converted" in {
      val message   = "Error message"
      val exception = new Exception(message)

      val nrsSubmissionError = NrsSubmissionError.InternalUnexpectedError(Some(exception))

      val result = nrsSubmissionErrorConverter.convert(nrsSubmissionError)

      result shouldBe ResponseError.internalServiceError(message = message, cause = Some(exception))
    }

    "return ResponseError.badRequestError when NrsSubmissionError.BadGateway is converted" in {
      val message = "Error message"

      val nrsSubmissionError = NrsSubmissionError.BadGateway(message, NOT_FOUND)

      val result = nrsSubmissionErrorConverter.convert(nrsSubmissionError)

      result shouldBe ResponseError.badGateway(message, NOT_FOUND)
    }
  }

  "valueOrError" should {
    "return ResponseError.internalServiceError when None is passed in" in {
      val result = await(valueOrError(None, "some value").value)

      result shouldBe Left(ResponseError.internalServiceError("Missing some value"))
    }
  }
}
