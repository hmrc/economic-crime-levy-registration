package uk.gov.hmrc.economiccrimelevyregistration

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._

class SubscriptionStatusISpec extends ISpecBase {

  s"GET ${routes.SubscriptionStatusController.getSubscriptionStatus(":businessPartnerId").url}" should {
    "return 200 OK with a subscribed ECL subscription status and the ECL registration reference" in {
      stubAuthorised()

      val eclSubscriptionStatus = EclSubscriptionStatus(
        Subscribed(testEclRegistrationReference)
      )

      stubGetSubscribedEclSubscriptionStatus()

      lazy val result =
        callRoute(FakeRequest(routes.SubscriptionStatusController.getSubscriptionStatus(testBusinessPartnerId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)
    }

    "return 200 OK with a not subscribed ECL subscription status" in {
      stubAuthorised()

      val eclSubscriptionStatus = EclSubscriptionStatus(
        subscriptionStatus = NotSubscribed
      )

      stubGetUnsubscribedEclSubscriptionStatus()

      lazy val result =
        callRoute(FakeRequest(routes.SubscriptionStatusController.getSubscriptionStatus(testBusinessPartnerId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)
    }
  }
}
