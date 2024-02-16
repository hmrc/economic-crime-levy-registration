/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.deregister

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes._

class DeregistrationISpec extends ISpecBase {

  s"PUT ${DeregistrationController.upsertDeregistration.url}"           should {
    "create or update a deregistration and return 200 OK with the deregistration" in {
      stubAuthorised()

      val deregistration = random[Deregistration]

      lazy val putResult = callRoute(
        FakeRequest(
          DeregistrationController.upsertDeregistration
        ).withJsonBody(Json.toJson(deregistration))
      )

      lazy val getResult =
        callRoute(
          FakeRequest(
            DeregistrationController
              .getDeregistration(deregistration.internalId)
          )
        )

      status(putResult)        shouldBe NO_CONTENT
      status(getResult)        shouldBe OK
      contentAsJson(getResult) shouldBe Json.toJson(deregistration.copy(lastUpdated = Some(now)))
    }
  }

  s"GET ${DeregistrationController.getDeregistration(":id").url}"       should {
    "return 200 OK with a deregistration that is already in the database" in {
      stubAuthorised()

      val deregistration = random[Deregistration]

      callRoute(
        FakeRequest(
          DeregistrationController.upsertDeregistration
        ).withJsonBody(Json.toJson(deregistration))
      ).futureValue

      lazy val result =
        callRoute(
          FakeRequest(
            DeregistrationController
              .getDeregistration(deregistration.internalId)
          )
        )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(deregistration.copy(lastUpdated = Some(now)))
    }

    "return 404 NOT_FOUND when trying to get a deregistration that doesn't exist" in {
      stubAuthorised()

      val deregistration      = random[Deregistration]
      val validDeregistration = deregistration.copy(internalId = "internalId")

      val result = callRoute(
        FakeRequest(
          DeregistrationController
            .getDeregistration(validDeregistration.internalId)
        )
      )

      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.toJson(
        ResponseError.notFoundError(s"Unable to find record with id: ${validDeregistration.internalId}")
      )
    }
  }

  s"DELETE ${DeregistrationController.deleteDeregistration(":id").url}" should {
    "delete a deregistration and return 200 OK" in {
      stubAuthorised()

      val deregistration = random[Deregistration]

      callRoute(
        FakeRequest(
          DeregistrationController.upsertDeregistration
        ).withJsonBody(Json.toJson(deregistration))
      ).futureValue

      lazy val getResultBeforeDelete =
        callRoute(
          FakeRequest(
            DeregistrationController
              .getDeregistration(deregistration.internalId)
          )
        )

      lazy val deleteResult =
        callRoute(
          FakeRequest(
            DeregistrationController
              .deleteDeregistration(deregistration.internalId)
          )
        )

      lazy val getResultAfterDelete =
        callRoute(
          FakeRequest(
            DeregistrationController
              .getDeregistration(deregistration.internalId)
          )
        )

      status(getResultBeforeDelete) shouldBe OK
      status(deleteResult)          shouldBe NO_CONTENT
      status(getResultAfterDelete)  shouldBe NOT_FOUND
    }
  }

}
