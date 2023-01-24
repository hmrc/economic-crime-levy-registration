/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.economiccrimelevyregistration.base

import uk.gov.hmrc.economiccrimelevyregistration.EclTestData

trait WireMockStubs extends EclTestData with AuthStubs with IntegrationFrameworkStubs with TaxEnrolmentsStubs
