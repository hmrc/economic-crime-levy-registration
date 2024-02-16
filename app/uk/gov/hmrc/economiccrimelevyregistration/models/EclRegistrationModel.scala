package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{Json, OFormat}

case class EclRegistrationModel(registration: Registration, additionalInfo: RegistrationAdditionalInfo)

object EclRegistrationModel {

  implicit val format: OFormat[EclRegistrationModel] = Json.format[EclRegistrationModel]

}
