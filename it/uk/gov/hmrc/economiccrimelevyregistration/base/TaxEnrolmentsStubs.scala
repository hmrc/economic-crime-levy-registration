package uk.gov.hmrc.economiccrimelevyregistration.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.CreateEnrolmentRequest

trait TaxEnrolmentsStubs { self: WireMockStubs =>

  def stubEnrol(createEnrolmentRequest: CreateEnrolmentRequest): StubMapping =
    stub(
      put(urlEqualTo("/tax-enrolments/service/HMRC-ECL-ORG/enrolment"))
        .withRequestBody(equalToJson(Json.toJson(createEnrolmentRequest).toString())),
      aResponse()
        .withStatus(204)
    )
}
