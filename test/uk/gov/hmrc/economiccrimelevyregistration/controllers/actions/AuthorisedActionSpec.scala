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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.actions

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{BodyParsers, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future

class AuthorisedActionSpec extends SpecBase {

  val defaultBodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector       = mock[AuthConnector]

  val authorisedAction =
    new BaseAuthorisedAction(mockAuthConnector, defaultBodyParser)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  private val expectedRetrievals =
    Retrievals.internalId and Retrievals.externalId and Retrievals.confidenceLevel and Retrievals.nino and Retrievals.saUtr and
      Retrievals.mdtpInformation and Retrievals.credentialStrength and Retrievals.loginTimes and
      Retrievals.credentials and Retrievals.name and Retrievals.dateOfBirth and Retrievals.email and
      Retrievals.affinityGroup and Retrievals.agentCode and Retrievals.agentInformation and Retrievals.credentialRole and Retrievals.groupIdentifier and
      Retrievals.itmpName and Retrievals.itmpDateOfBirth and Retrievals.itmpAddress

  "invokeBlock" should {
    "execute the block and return the result if authorised" in forAll(
      arbAuthRetrievals(Some(alphaNumericString)).arbitrary
    ) { authRetrievals: AuthRetrievals =>
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(authRetrievals))

      val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Test"
    }

    "return 401 unauthorized if there is an authorisation exception" in {
      List(
        InsufficientConfidenceLevel(),
        InsufficientEnrolments(),
        UnsupportedAffinityGroup(),
        UnsupportedCredentialRole(),
        UnsupportedAuthProvider(),
        IncorrectCredentialStrength(),
        InternalError(),
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result) shouldBe UNAUTHORIZED
      }
    }

    "throw an UnauthorizedException if there is no internal id" in forAll(arbAuthRetrievals(None).arbitrary) {
      authRetrievals =>
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(Future(authRetrievals))

        val result = intercept[UnauthorizedException] {
          await(authorisedAction.invokeBlock(fakeRequest, testAction))
        }

        result.message shouldBe "Unable to retrieve internalId"
    }
  }

}
