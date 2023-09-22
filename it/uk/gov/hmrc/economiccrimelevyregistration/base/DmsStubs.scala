package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._

trait DmsStubs { self: WireMockStubs =>
  def stubDmsSuccess(): StubMapping =
    stub(
      post(urlEqualTo("/dms-submission/submit")),
      aResponse()
        .withStatus(ACCEPTED)
    )

  def stubDms5xx(): StubMapping =
    stub(
      post(urlEqualTo("/dms-submission/submit")),
      aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)
        .withBody("Internal server error")
    )

}
