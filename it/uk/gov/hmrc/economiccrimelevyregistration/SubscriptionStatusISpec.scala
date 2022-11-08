package uk.gov.hmrc.economiccrimelevyregistration

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{Rejected, Successful}

class SubscriptionStatusISpec extends ISpecBase {

  s"GET /$contextPath/subscription-status/:businessPartnerId" should {

    "return 200 OK with a successful ECL subscription status" in {
      stubAuthorised()

      val businessPartnerId        = testBusinessPartnerId
      val eclRegistrationReference = testEclRegistrationReference

      val eclSubscriptionStatus = EclSubscriptionStatus(
        subscriptionStatus = Successful,
        eclRegistrationReference = Some(eclRegistrationReference)
      )

      stubGetSuccessfulEclSubscriptionStatus(businessPartnerId, eclRegistrationReference)

      lazy val result =
        callRoute(FakeRequest(routes.SubscriptionStatusController.getSubscriptionStatus(businessPartnerId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)
    }

    "return 200 OK with a successful non-ECL subscription status" in {
      stubAuthorised()

      val businessPartnerId = testBusinessPartnerId

      val eclSubscriptionStatus = EclSubscriptionStatus(
        subscriptionStatus = Successful,
        eclRegistrationReference = None
      )

      stubGetSuccessfulNonEclSubscriptionStatus(businessPartnerId, testOtherRegimeRegistrationReference)

      lazy val result =
        callRoute(FakeRequest(routes.SubscriptionStatusController.getSubscriptionStatus(businessPartnerId)))

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)
    }

    "return 200 OK with an unsuccessful ECL subscription status" in {
      stubAuthorised()

      val businessPartnerId = testBusinessPartnerId

      val eclSubscriptionStatus = EclSubscriptionStatus(
        subscriptionStatus = Rejected,
        eclRegistrationReference = None
      )

      stubGetUnsuccessfulEclSubscriptionStatus(businessPartnerId)

      lazy val result =
        callRoute(FakeRequest(routes.SubscriptionStatusController.getSubscriptionStatus(businessPartnerId)))

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)
    }

  }
}
