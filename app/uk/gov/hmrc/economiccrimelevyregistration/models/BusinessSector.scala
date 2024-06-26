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

import play.api.libs.json._

sealed trait BusinessSector

object BusinessSector {
  case object Auditor extends BusinessSector

  case object CreditInstitution extends BusinessSector

  case object CryptoAssetExchangeProvider extends BusinessSector

  case object EstateAgentOrLettingAgent extends BusinessSector

  case object ExternalAccountant extends BusinessSector

  case object FinancialInstitution extends BusinessSector

  case object HighValueDealer extends BusinessSector

  case object IndependentLegalProfessional extends BusinessSector

  case object InsolvencyPractitioner extends BusinessSector

  case object TaxAdviser extends BusinessSector

  case object TrustOrCompanyServiceProvider extends BusinessSector

  implicit val format: Format[BusinessSector] = new Format[BusinessSector] {
    override def reads(json: JsValue): JsResult[BusinessSector] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Auditor"                       => JsSuccess(Auditor)
          case "CreditInstitution"             => JsSuccess(CreditInstitution)
          case "CryptoAssetExchangeProvider"   => JsSuccess(CryptoAssetExchangeProvider)
          case "EstateAgentOrLettingAgent"     => JsSuccess(EstateAgentOrLettingAgent)
          case "ExternalAccountant"            => JsSuccess(ExternalAccountant)
          case "FinancialInstitution"          => JsSuccess(FinancialInstitution)
          case "HighValueDealer"               => JsSuccess(HighValueDealer)
          case "IndependentLegalProfessional"  => JsSuccess(IndependentLegalProfessional)
          case "InsolvencyPractitioner"        => JsSuccess(InsolvencyPractitioner)
          case "TaxAdviser"                    => JsSuccess(TaxAdviser)
          case "TrustOrCompanyServiceProvider" => JsSuccess(TrustOrCompanyServiceProvider)
          case s                               => JsError(s"$s is not a valid BusinessSector")
        }
      case e: JsError          => e
    }

    override def writes(o: BusinessSector): JsValue = JsString(o.toString)
  }
}
