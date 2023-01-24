package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponse

trait IntegrationFrameworkStubs { self: WireMockStubs =>

  def stubGetSubscribedEclSubscriptionStatus(): StubMapping =
    stub(
      get(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$testBusinessPartnerId/status")),
      aResponse()
        .withStatus(200)
        .withBody(
          s"""
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
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "subscriptionStatus": "NO_FORM_BUNDLE_FOUND"
             |}
     """.stripMargin)
    )

  def stubSubscribeToEcl(businessPartnerId: String, subscriptionResponse: CreateEclSubscriptionResponse): StubMapping =
    stub(
      post(urlEqualTo(s"/economic-crime-levy/subscriptions/ECL/create?idType=SAFE&idValue=$businessPartnerId")),
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(subscriptionResponse).toString())
    )
}
