package uk.gov.hmrc.economiccrimelevyregistration

import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, matching, urlEqualTo, verify}
import org.scalacheck.Arbitrary
import play.api.{Application, Mode}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CustomHeaderNames, EclSubscriptionStatus}
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus.{NotSubscribed, Subscribed}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{GetSubscriptionResponse, HipGetSubscriptionResponse}
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.*
import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CustomHeaderNames, EclSubscriptionStatus}
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus.*
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.GetSubscriptionResponse
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.*

import java.time.Clock

class HipSubscriptionStatusISpec  extends ISpecBase {

  val config: Map[String, Any] = Map(
    "features.hip.subscriptions" -> true
  ) ++ additionalAppConfig

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(config)
      .overrides(bind(classOf[Clock]).toInstance(stubClock))
      .in(Mode.Test)
      .build()

  val totalNumberOfCalls = 4

  val correlationidHeaderName = "correlationid"

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
          .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
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
          .withHeader(CustomHeaderNames.xCorrelationId, matching(uuidRegex))
      )
    }
  }

  s"GET ${routes.SubscriptionController.getSubscription(":eclReference").url}"             should {
    "return 200 OK with a subscription for provided eclReference" in {
      stubAuthorised()

      val response =
        LazyList.continually(Arbitrary.arbitrary[HipGetSubscriptionResponse].sample).flatten.head

      stubHipGetSubscription(response)

      val result =
        callRoute(FakeRequest(routes.SubscriptionController.getSubscription(testEclRegistrationReference)))

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(response.success)

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/etmp/RESTAdaptor/economic-crime-levy/subscription/$testEclRegistrationReference"))
          .withHeader(correlationidHeaderName, matching(uuidRegex))
      )
    }

    "retry 3 times and return 500 INTERNAL_SERVER_ERROR when call to hip connector fails" in {
      stubAuthorised()

      stubHipGetSubscriptionFailed()

      val result =
        callRoute(FakeRequest(routes.SubscriptionController.getSubscription(testEclRegistrationReference)))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(
        totalNumberOfCalls,
        getRequestedFor(urlEqualTo(s"/etmp/RESTAdaptor/economic-crime-levy/subscription/$testEclRegistrationReference"))
          .withHeader(correlationidHeaderName, matching(uuidRegex))
      )
    }
  }
}

