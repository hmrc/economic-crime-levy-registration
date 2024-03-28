package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.GetSubscriptionResponse
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.utils.HttpConstants

class SubscriptionStatusISpec extends ISpecBase {

  val totalNumberOfCalls = 4

  s"GET ${routes.SubscriptionController.getSubscriptionStatus(":idType", ":idValue").url}" should {
    "return 200 OK with a subscribed ECL subscription status and the ECL registration reference" in {
      stubAuthorised()

      val eclSubscriptionStatus = EclSubscriptionStatus(
        Subscribed(testEclRegistrationReference)
      )

      stubGetSubscribedEclSubscriptionStatus()

      lazy val result =
        callRoute(FakeRequest(routes.SubscriptionController.getSubscriptionStatus("SAFE", testBusinessPartnerId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$testBusinessPartnerId/status"))
          .withHeader(HttpConstants.HEADER_X_CORRELATION_ID, matching(uuidRegex))
      )
    }

    "return 200 OK with a not subscribed ECL subscription status" in {
      stubAuthorised()

      val eclSubscriptionStatus = EclSubscriptionStatus(
        subscriptionStatus = NotSubscribed
      )

      stubGetUnsubscribedEclSubscriptionStatus()

      lazy val result =
        callRoute(FakeRequest(routes.SubscriptionController.getSubscriptionStatus("SAFE", testBusinessPartnerId)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(eclSubscriptionStatus)

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$testBusinessPartnerId/status"))
          .withHeader(HttpConstants.HEADER_X_CORRELATION_ID, matching(uuidRegex))
      )
    }
  }

  s"GET ${routes.SubscriptionController.getSubscription(":eclReference").url}"             should {
    "return 200 OK with a subscription for provided eclReference" in {
      stubAuthorised()

      val response = random[GetSubscriptionResponse]

      stubGetSubscription(response)

      val result =
        callRoute(FakeRequest(routes.SubscriptionController.getSubscription(testEclRegistrationReference)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(response)

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/economic-crime-levy/subscription/$testEclRegistrationReference"))
          .withHeader(HttpConstants.HEADER_X_CORRELATION_ID, matching(uuidRegex))
      )
    }

    "retry 3 times and return 500 INTERNAL_SERVER_ERROR when call to integrationFrameworkConnector fails" in {
      stubAuthorised()

      stubGetSubscriptionFailed()

      val result =
        callRoute(FakeRequest(routes.SubscriptionController.getSubscription(testEclRegistrationReference)))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(
        totalNumberOfCalls,
        getRequestedFor(urlEqualTo(s"/economic-crime-levy/subscription/$testEclRegistrationReference"))
          .withHeader(HttpConstants.HEADER_X_CORRELATION_ID, matching(uuidRegex))
      )
    }
  }
}
