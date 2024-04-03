package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.dms.{DmsNotification, SubmissionItemStatus}
import uk.gov.hmrc.http.HeaderNames

class DmsNotificationISpec extends ISpecBase {
  val dmsNotification: DmsNotification = random[DmsNotification]

  s"POST ${routes.DmsNotificationController.dmsCallback().url}" should {
    "process a notification message from DMS" in {
      stubInternalAuthorised()

      val result = callRoute(
        FakeRequest(routes.DmsNotificationController.dmsCallback())
          .withJsonBody(Json.toJson(dmsNotification))
          .withHeaders(HeaderNames.authorisation -> "Token some-token")
      )

      status(result) shouldBe OK
    }
  }

  s"POST ${routes.DmsNotificationController.dmsCallback().url}" should {
    "process a notification message from DMS when the status equals failed" in {
      stubInternalAuthorised()

      val result = callRoute(
        FakeRequest(routes.DmsNotificationController.dmsCallback())
          .withJsonBody(Json.toJson(dmsNotification.copy(status = SubmissionItemStatus.Failed)))
          .withHeaders(HeaderNames.authorisation -> "Token some-token")
      )

      status(result) shouldBe OK
    }
  }

  s"POST ${routes.DmsNotificationController.dmsCallback().url}" should {
    "return BadRequest when response from DMS can't be deserialized" in {
      stubInternalAuthorised()

      val result = callRoute(
        FakeRequest(routes.DmsNotificationController.dmsCallback())
          .withJsonBody(Json.toJson("Invalid json"))
          .withHeaders(HeaderNames.authorisation -> "Token some-token")
      )

      status(result) shouldBe BAD_REQUEST
    }
  }

}
