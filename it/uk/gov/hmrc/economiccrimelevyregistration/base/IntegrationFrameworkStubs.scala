package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.{CreateEclSubscriptionResponse, EclSubscription, GetSubscriptionResponse}

trait IntegrationFrameworkStubs { self: WireMockStubs =>

  def stubGetSubscribedEclSubscriptionStatus(): StubMapping =
    stub(
      get(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$testBusinessPartnerId/status")),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "subscriptionStatus": "SUCCESSFUL",
             |  "idType": "ZECL",
             |  "idValue": "$testEclRegistrationReference",
             |  "channel": "Online"
             |}
       """.stripMargin)
    )

  def stubGetUnsubscribedEclSubscriptionStatus(): StubMapping =
    stub(
      get(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$testBusinessPartnerId/status")),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "subscriptionStatus": "NO_FORM_BUNDLE_FOUND"
             |}
     """.stripMargin)
    )

  def stubSubscribeToEcl(
    eclSubscription: EclSubscription,
    subscriptionResponse: CreateEclSubscriptionResponse
  ): StubMapping =
    stub(
      post(urlEqualTo(s"/economic-crime-levy/subscription/${eclSubscription.businessPartnerId}"))
        .withRequestBody(equalToJson(Json.toJson(eclSubscription.subscription).toString())),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(subscriptionResponse).toString())
    )

  def stubGetSubscription(
    getSubscriptionResponse: GetSubscriptionResponse
  ): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy/subscription/$testEclRegistrationReference")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(getSubscriptionResponse).toString())
    )

  def stubGetSubscriptionFailed(): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy/subscription/$testEclRegistrationReference")),
      aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)
        .withBody("")
    )
}
