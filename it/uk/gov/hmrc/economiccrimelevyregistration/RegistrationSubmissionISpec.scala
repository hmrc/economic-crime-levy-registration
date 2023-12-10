package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest.concurrent.Eventually.eventually
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Other
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.{Base64EncodedFields, KeyValue, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{CreateEnrolmentRequest, EclEnrolment}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import java.util.Base64

class RegistrationSubmissionISpec extends ISpecBase {
  s"POST ${routes.RegistrationSubmissionController.submitRegistration(":id").url}" should {
    "return 200 OK when the registration data for 'other' entity is valid" in {
      stubAuthorised()

      val html                = "<html><head></head><body></body></html>"
      val charityRegistration = random[ValidCharityRegistration]
      val validRegistration   = charityRegistration.copy(
        registration = charityRegistration.registration.copy(
          registrationType = Some(Initial),
          base64EncodedFields = Some(
            Base64EncodedFields(
              Some(Base64.getEncoder.encodeToString(html.getBytes)),
              Some(Base64.getEncoder.encodeToString(html.getBytes))
            )
          )
        )
      )

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      ).futureValue

      callRoute(
        FakeRequest(routes.RegistrationAdditionalInfoController.upsert).withJsonBody(
          Json.toJson(validRegistration.registrationAdditionalInfo)
        )
      ).futureValue

      stubDmsSuccess()

      val result = callRoute(
        FakeRequest(
          routes.RegistrationSubmissionController.submitRegistration(validRegistration.registration.internalId)
        ).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      )

      status(result) shouldBe OK
      verify(1, postRequestedFor(urlEqualTo("/dms-submission/submit")))
    }

    "return BAD_GATEWAY if call to DMS fails after three retries" in {
      stubAuthorised()

      val html                = "<html><head></head><body></body></html>"
      val charityRegistration = random[ValidCharityRegistration]
      val validRegistration   = charityRegistration.copy(
        registration = charityRegistration.registration.copy(
          base64EncodedFields = Some(Base64EncodedFields(None, Some(Base64.getEncoder.encodeToString(html.getBytes)))),
          registrationType = Some(Initial)
        )
      )

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      ).futureValue

      callRoute(
        FakeRequest(routes.RegistrationAdditionalInfoController.upsert).withJsonBody(
          Json.toJson(validRegistration.registrationAdditionalInfo)
        )
      ).futureValue

      stubDms5xx()

      val result = callRoute(
        FakeRequest(
          routes.RegistrationSubmissionController.submitRegistration(validRegistration.registration.internalId)
        ).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      )

      status(result) shouldBe BAD_GATEWAY
      verify(4, postRequestedFor(urlEqualTo("/dms-submission/submit")))
    }
  }
}
