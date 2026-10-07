package uk.gov.hmrc.economiccrimelevyregistration

import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, matching, urlEqualTo, verify}
import org.scalacheck.Arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{FakeRequest, FutureAwaits}
import play.api.{Application, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.base.{ISpecBase, WireMockHelper}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.*
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus.*
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SubscriptionSubmissionError
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, GetSubscriptionResponse, HipGetSubscriptionResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.{CustomHeaderNames, EclSubscriptionStatus}
import uk.gov.hmrc.economiccrimelevyregistration.services.SubscriptionService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock

class HipSubscriptionsISpec extends ISpecBase with FutureAwaits with ScalaCheckPropertyChecks {

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
  val subscriptionService     = app.injector.instanceOf[SubscriptionService]

  "Subscription service" should {
    "return error with code 400 " in {
      forAll { (args: ValidSoleTraderRegistration) =>
        val safeId       = args.expectedEclSubscription.businessPartnerId
        val expectedJson =
          """
            |{
            |  "origin": "HoD",
            |  "response": {
            |    "error": {
            |      "code": "400",
            |      "logID": "D82EBAB67AC6D7565C0682CA91BDC577",
            |      "message": "Submission has not passed validation."
            |    }
            |  }
            |}
            |""".stripMargin

        WireMockHelper.stubPost(
          s"/etmp/RESTAdaptor/economic-crime-levy/subscription/$safeId",
          400,
          expectedJson,
          ("foo", "bar")
        )

        val result: Either[SubscriptionSubmissionError, CreateEclSubscriptionResponse] =
          await(
            subscriptionService
              .executeCallToSubscriptionCreateApi(
                args.expectedEclSubscription,
                args.registration,
                None
              )(HeaderCarrier())
              .value
          )

        result.isLeft shouldBe true
        val Some(SubscriptionSubmissionError.BadGateway(message, code)) = result.left.toOption: @unchecked
        code    shouldBe 400
        message shouldBe expectedJson
      }
    }

    "return success" in {
      forAll { (args: ValidSoleTraderRegistration) =>
        val safeId       = args.expectedEclSubscription.businessPartnerId
        val expectedJson =
          """
            |{
            |  "success": {
            |    "processingDate": "2001-12-17T09:30:47Z",
            |    "formBundleNumber": "12345678912",
            |    "eclReference": "XMECL1234567817"
            |  }
            |}
            |""".stripMargin

        val expected: CreateEclSubscriptionResponse = Json.parse(expectedJson).as[CreateEclSubscriptionResponse]

        WireMockHelper.stubPost(
          s"/etmp/RESTAdaptor/economic-crime-levy/subscription/$safeId",
          201,
          expectedJson,
          ("foo", "bar")
        )

        val result: Either[SubscriptionSubmissionError, CreateEclSubscriptionResponse] =
          await(
            subscriptionService
              .executeCallToSubscriptionCreateApi(
                args.expectedEclSubscription,
                args.registration,
                None
              )(HeaderCarrier())
              .value
          )

        result.isRight  shouldBe true
        result.toOption shouldBe Some(expected)
      }
    }
  }

  // n.b. this api is not yet migrated to hip
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
