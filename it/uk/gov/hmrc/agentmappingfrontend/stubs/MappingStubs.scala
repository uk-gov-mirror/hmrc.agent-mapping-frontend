package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model.{Identifier, Mapping, Mappings}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

object MappingStubs {

  val listOfMapping = List(
    Mapping("ARN0001", Identifier("IRAgentReference", "AgentCode1")),
    Mapping("ARN0001", Identifier("IRAgentReference", "AgentCode2")))

  val jsonBody = Json.toJson(Mappings(listOfMapping))

  def mappingIsCreated(utr: Utr, arn: Arn, identifiers: Seq[Identifier]): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/${utr.value}/${arn.value}/${identifiers.mkString("~")}"))
        willReturn aResponse().withStatus(201))
  }

  def mappingExists(utr: Utr, arn: Arn, identifiers: Seq[Identifier]): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/${utr.value}/${arn.value}/${identifiers.mkString("~")}"))
      willReturn aResponse().withStatus(409))
  }

  def mappingKnownFactsIssue(utr: Utr, arn: Arn, identifiers: Seq[Identifier]): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/${utr.value}/${arn.value}/${identifiers.mkString("~")}"))
      willReturn aResponse().withStatus(403))
  }

  def mappingsFound(arn:Arn): Unit = {
    stubFor(get(urlPathEqualTo(s"/agent-mapping/mappings/${arn.value}"))
      .willReturn(aResponse().withStatus(200).withBody(jsonBody.toString())))
  }

  def noMappingsFound(arn:Arn): Unit = {
    stubFor(get(urlPathEqualTo(s"/agent-mapping/mappings/${arn.value}"))
      .willReturn(aResponse().withStatus(404)))
  }

  def mappingsDelete(arn:Arn): Unit = {
    stubFor(delete(urlPathEqualTo(s"/agent-mapping/test-only/mappings/${arn.value}"))
      .willReturn(aResponse().withStatus(204)))
 }
}
