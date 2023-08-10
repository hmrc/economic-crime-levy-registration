package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.dms.DmsNotification

class DmsNotificationISpec extends ISpecBase {
  s"POST ${routes.DmsNotificationController.dmsCallback().url}" should {
    "process a notification message from DMS" in {
      stubInternalAuthorised()

      val result = callRoute(
        FakeRequest(routes.DmsNotificationController.dmsCallback())
          .withJsonBody(Json.toJson(random[DmsNotification]))
          .withHeaders(AUTHORIZATION -> "Token some-token")
      )

      status(result)        shouldBe OK
    }
  }

}
