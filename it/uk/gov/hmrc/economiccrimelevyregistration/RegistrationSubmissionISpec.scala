package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.KeyValue
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{CreateEnrolmentRequest, EclEnrolment}
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CreateEclSubscriptionResponse

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

class RegistrationSubmissionISpec extends ISpecBase {
  s"POST ${routes.RegistrationSubmissionController.submitRegistration(":id").url}" should {
    "return 200 OK with a subscription reference number in the JSON response body when the registration data is valid" in {
      stubAuthorised()

      val validRegistration    = random[ValidUkCompanyRegistration]
      val subscriptionResponse =
        random[CreateEclSubscriptionResponse].copy(processingDate = Instant.parse("2007-12-25T10:15:30.00Z"))

      val registrationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

      stubSubscribeToEcl(
        validRegistration.expectedEclSubscription.copy(subscription =
          validRegistration.expectedEclSubscription.subscription.copy(
            legalEntityDetails = validRegistration.expectedEclSubscription.subscription.legalEntityDetails
              .copy(registrationDate = registrationDate)
          )
        ),
        subscriptionResponse
      )

      stubEnrol(
        CreateEnrolmentRequest(
          identifiers = Seq(KeyValue(key = EclEnrolment.IdentifierKey, value = subscriptionResponse.eclReference)),
          verifiers = Seq(KeyValue(key = EclEnrolment.VerifierKey, value = "20071225"))
        )
      )

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
      contentAsJson(result) shouldBe Json.toJson(subscriptionResponse)
    }
  }

}
