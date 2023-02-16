/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.models

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.integrationframework.CorrespondenceAddressDetails

class CorrespondenceAddressDetailsSpec extends SpecBase {
  private val organisation: String     = "Test organisation"
  private val addressLine1: String     = "Test line 1"
  private val addressLine2: String     = "Test line 2"
  private val addressLine3: String     = "Test line 3"
  private val addressLine4: String     = "Test line 4"
  private val region: String           = "Test region"
  private val postcode: Option[String] = Some("AB12 3CD")
  private val poBox: String            = "PO box 123"
  private val countryCode: String      = "GB"
  private val longAddressLine1: String = "Test line 1 that is longer than 35 characters"
  private val longAddressLine2: String = "Test line 2 that is longer than 35 characters"
  private val longAddressLine3: String = "Test line 3 that is longer than 35 characters"
  private val longAddressLine4: String = "Test line 4 that is longer than 35 characters"

  val lines1: (String, Seq[String]) = (organisation, Seq(addressLine1, addressLine2, region))
  val lines2: (String, Seq[String]) =
    (organisation, Seq(addressLine1, addressLine2, addressLine3, addressLine4, region, poBox))
  val lines3: (String, Seq[String]) =
    (addressLine1, Seq(addressLine2, addressLine3, addressLine4))
  val lines4: (String, Seq[String]) =
    (longAddressLine1, Seq(addressLine2, addressLine3, addressLine4))
  val lines5: (String, Seq[String]) =
    (longAddressLine1, Seq(longAddressLine2, longAddressLine3, longAddressLine4))

  val correspondenceAddressDetails1: CorrespondenceAddressDetails = CorrespondenceAddressDetails(
    addressLine1 = organisation,
    addressLine2 = Some(addressLine1),
    addressLine3 = Some(addressLine2),
    addressLine4 = Some(region),
    postCode = postcode,
    country = Some(countryCode)
  )

  val correspondenceAddressDetails2: CorrespondenceAddressDetails = CorrespondenceAddressDetails(
    addressLine1 = organisation,
    addressLine2 = Some(s"$addressLine1, $addressLine4"),
    addressLine3 = Some(s"$addressLine2, $region"),
    addressLine4 = Some(s"$addressLine3, $poBox"),
    postCode = postcode,
    country = Some(countryCode)
  )

  val correspondenceAddressDetails3: CorrespondenceAddressDetails = CorrespondenceAddressDetails(
    addressLine1 = addressLine1,
    addressLine2 = Some(addressLine2),
    addressLine3 = Some(addressLine3),
    addressLine4 = Some(addressLine4),
    postCode = postcode,
    country = Some(countryCode)
  )

  val correspondenceAddressDetails4: CorrespondenceAddressDetails = CorrespondenceAddressDetails(
    addressLine1 = "Test line 1 that is longer than 35 ",
    addressLine2 = Some(s"characters, $addressLine2"),
    addressLine3 = Some(addressLine3),
    addressLine4 = Some(addressLine4),
    postCode = postcode,
    country = Some(countryCode)
  )

  val correspondenceAddressDetails5: CorrespondenceAddressDetails = CorrespondenceAddressDetails(
    addressLine1 = "Test line 1 that is longer than 35 ",
    addressLine2 = Some("characters, Test line 2 that is lon"),
    addressLine3 = Some("ger than 35 characters, Test line 3"),
    addressLine4 = Some(" that is longer than 35 characters,"),
    postCode = postcode,
    country = Some(countryCode)
  )

  "apply" should {
    "construct correspondence address details from the given address lines, postcode and country code" in forAll(
      Table(
        ("lines", "correspondenceAddressDetails"),
        (lines1, correspondenceAddressDetails1),
        (lines2, correspondenceAddressDetails2),
        (lines3, correspondenceAddressDetails3),
        (lines4, correspondenceAddressDetails4),
        (lines5, correspondenceAddressDetails5)
      )
    ) { (lines: (String, Seq[String]), correspondenceAddressDetails: CorrespondenceAddressDetails) =>
      CorrespondenceAddressDetails(lines._1, lines._2, postcode, countryCode) shouldBe correspondenceAddressDetails
    }

  }

}
