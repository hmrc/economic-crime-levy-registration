package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest.concurrent.Eventually.eventually
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{Base64EncodedFields, KeyValue}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{CreateEnrolmentRequest, EclEnrolment}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.LegalEntityDetails.StartOfFirstEclFinancialYear
import uk.gov.hmrc.economiccrimelevyregistration.models.nrs._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import java.util.Base64

class RegistrationSubmissionISpec extends ISpecBase {
  s"POST ${routes.RegistrationSubmissionController.submitRegistration(":id").url}" should {
    "return 200 OK with a subscription reference number in the JSON response body when the registration data is valid" in {
      stubAuthorised()

      val validRegistration    = random[ValidIncorporatedEntityRegistration]
      val subscriptionResponse = CreateEclSubscriptionResponse(success =
        random[CreateEclSubscriptionResponse].success.copy(processingDate = Instant.parse("2007-12-25T10:15:30Z"))
      )

      val registrationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

      stubSubscribeToEcl(
        validRegistration.expectedEclSubscription.copy(subscription =
          validRegistration.expectedEclSubscription.subscription.copy(
            legalEntityDetails = validRegistration.expectedEclSubscription.subscription.legalEntityDetails
              .copy(registrationDate = registrationDate, liabilityStartDate = StartOfFirstEclFinancialYear)
          )
        ),
        subscriptionResponse
      )

      stubEnrol(
        CreateEnrolmentRequest(
          identifiers =
            Seq(KeyValue(key = EclEnrolment.IdentifierKey, value = subscriptionResponse.success.eclReference)),
          verifiers = Seq(KeyValue(key = EclEnrolment.VerifierKey, value = "20071225"))
        )
      )

      val nrsSubmissionResponse = random[NrsSubmissionResponse]

      stubNrsSuccess(nrsSubmissionResponse)

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      ).futureValue

      val result = callRoute(
        FakeRequest(
          routes.RegistrationSubmissionController.submitRegistration(validRegistration.registration.internalId)
        ).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)

      eventually {
        verify(1, postRequestedFor(urlEqualTo("/submission")))
      }
    }

    "retry the NRS submission call 3 times after the initial attempt if it fails with a 5xx response" in {
      stubAuthorised()

      val validRegistration    = random[ValidIncorporatedEntityRegistration]
      val subscriptionResponse = CreateEclSubscriptionResponse(success =
        random[CreateEclSubscriptionResponse].success.copy(processingDate = Instant.parse("2007-12-25T10:15:30Z"))
      )

      val registrationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

      stubSubscribeToEcl(
        validRegistration.expectedEclSubscription.copy(subscription =
          validRegistration.expectedEclSubscription.subscription.copy(
            legalEntityDetails = validRegistration.expectedEclSubscription.subscription.legalEntityDetails
              .copy(registrationDate = registrationDate, liabilityStartDate = StartOfFirstEclFinancialYear)
          )
        ),
        subscriptionResponse
      )

      stubEnrol(
        CreateEnrolmentRequest(
          identifiers =
            Seq(KeyValue(key = EclEnrolment.IdentifierKey, value = subscriptionResponse.success.eclReference)),
          verifiers = Seq(KeyValue(key = EclEnrolment.VerifierKey, value = "20071225"))
        )
      )

      stubNrs5xx()

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      ).futureValue

      val result = callRoute(
        FakeRequest(
          routes.RegistrationSubmissionController.submitRegistration(validRegistration.registration.internalId)
        ).withJsonBody(
          Json.toJson(validRegistration.registration)
        )
      )

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(subscriptionResponse.success)

      eventually {
        verify(4, postRequestedFor(urlEqualTo("/submission")))
      }
    }

    "return 200 OK when the registration data for 'other' entity is valid" in {
      stubAuthorised()

      val html                = "<html><head></head><body></body></html>"
      val charityRegistration = random[ValidCharityRegistration]
      val validRegistration   = charityRegistration.copy(
        registration = charityRegistration.registration.copy(
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

    "return INTERNAL_SERVER_ERROR if call to DMS fails after three retries" in {
      stubAuthorised()

      val html                = "<html><head></head><body></body></html>"
      val charityRegistration = random[ValidCharityRegistration]
      val validRegistration   = charityRegistration.copy(
        registration = charityRegistration.registration.copy(
          base64EncodedFields = Some(Base64EncodedFields(None, Some(Base64.getEncoder.encodeToString(html.getBytes))))
        )
      )

      callRoute(
        FakeRequest(routes.RegistrationController.upsertRegistration).withJsonBody(
          Json.toJson(validRegistration.registration)
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

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(4, postRequestedFor(urlEqualTo("/dms-submission/submit")))
    }
  }
}
