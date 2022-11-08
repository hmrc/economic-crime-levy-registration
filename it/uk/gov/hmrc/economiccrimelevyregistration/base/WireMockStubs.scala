/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._

trait WireMockStubs {

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

  def stubGetSuccessfulEclSubscriptionStatus(businessPartnerId: String, eclRegistrationReference: String): StubMapping =
    stub(
      get(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$businessPartnerId/status")),
      aResponse()
        .withStatus(200)
        .withBody(s"""
             |{
             |  "subscriptionStatus": "SUCCESSFUL",
             |  "idType": "ZECL",
             |  "idValue": "$eclRegistrationReference",
             |  "channel": "Online"
             |}
         """.stripMargin)
    )

  def stubGetSuccessfulNonEclSubscriptionStatus(
    businessPartnerId: String,
    otherRegimeRegistrationReference: String
  ): StubMapping =
    stub(
      get(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$businessPartnerId/status")),
      aResponse()
        .withStatus(200)
        .withBody(s"""
             |{
             |  "subscriptionStatus": "SUCCESSFUL",
             |  "idType": "ZPPT",
             |  "idValue": "$otherRegimeRegistrationReference",
             |  "channel": "Online"
             |}
       """.stripMargin)
    )

  def stubGetUnsuccessfulEclSubscriptionStatus(businessPartnerId: String): StubMapping =
    stub(
      get(urlEqualTo(s"/cross-regime/subscription/ECL/SAFE/$businessPartnerId/status")),
      aResponse()
        .withStatus(200)
        .withBody(s"""
             |{
             |  "subscriptionStatus": "REJECTED",
             |  "idType": "ZECL"
             |}
       """.stripMargin)
    )

}
