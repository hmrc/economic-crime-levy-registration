package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.internalauth.client.{Resource, ResourceLocation, ResourceType}

trait AuthStubs { self: WireMockStubs =>
  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise")),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "internalId": "id",
             |  "loginTimes": {
             |     "currentLogin": "2016-11-27T09:00:00.000Z",
             |     "previousLogin": "2016-11-01T12:00:00.000Z"
             |  },
             |  "agentInformation": {},
             |  "confidenceLevel": 50
             |}
         """.stripMargin)
    )

  private def responseJsonStr(
                               resourceType     : Option[ResourceType],
                               responseResources: Set[Resource],
                             ) = {
    responseResources
      .map { resource =>
        s"""{
          "resourceType":"${resource.resourceType.value}",
          "resourceLocation": "${resource.resourceLocation.value}"
        }"""
      }
      .mkString("[", ",", "]")
  }

  def stubInternalAuthorised(resourceType: String): StubMapping =
    stub(
      post(urlEqualTo("/internal-auth/auth")),
      aResponse()
        .withStatus(OK)
        .withBody(responseJsonStr(
          Some(ResourceType(resourceType)),
          Set(Resource(
            ResourceType("dms-submission"),
            ResourceLocation("dms-registration-callback")
          ))
        ))
        .withHeader("Content-Type", "application/json")
    )
}
