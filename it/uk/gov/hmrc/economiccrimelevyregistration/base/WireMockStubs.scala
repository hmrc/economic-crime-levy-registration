/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyregistration.EclTestData
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._

trait WireMockStubs extends EclTestData {

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
             |{
             |  "authorise": [],
             |  "retrieve": [ "internalId" ]
             |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(200)
        .withBody(s"""
                     |{
                     |  "internalId": "id"
                     |}
           """.stripMargin)
    )

  def stubGetSubscribedEclSubscriptionStatus(): StubMapping =
    stub(
      get(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$testBusinessPartnerId/status")),
      aResponse()
        .withStatus(200)
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
        .withStatus(200)
        .withBody(s"""
             |{
             |  "subscriptionStatus": "NO_FORM_BUNDLE_FOUND"
             |}
       """.stripMargin)
    )

}
