package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._

trait NrsStubs { self: WireMockStubs =>
  def stubNrsSuccess(): StubMapping =
    stub(
      post(urlEqualTo("/submission")),
      aResponse()
        .withStatus(ACCEPTED)
    )

  def stubNrs5xx(): StubMapping =
    stub(
      post(urlEqualTo("/submission")),
      aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)
        .withBody("Internal server error")
    )

}
