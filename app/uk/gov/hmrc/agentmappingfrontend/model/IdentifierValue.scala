package uk.gov.hmrc.agentmappingfrontend.model

import play.api.libs.json.Json

case class IdentifierValue(value: String)

object IdentifierValue {
  implicit val format = Json.format[IdentifierValue]
}
