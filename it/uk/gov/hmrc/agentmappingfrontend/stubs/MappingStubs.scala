package uk.gov.hmrc.agentmappingfrontend.stubs

import uk.gov.hmrc.domain.SaAgentReference
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model.{Mapping, Mappings}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

object MappingStubs {

  val listOfMapping = List(
    Mapping("ARN0001", "AgentCode1"),
    Mapping("ARN0001", "AgentCode2"))

  val jsonBody = Json.toJson(Mappings(listOfMapping))

  def mappingIsCreated(utr: Utr, arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/${utr.value}/${arn.value}/$saAgentReference"))
        willReturn aResponse().withStatus(201))
  }

  def mappingExists(utr: Utr, arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/${utr.value}/${arn.value}/$saAgentReference"))
      willReturn aResponse().withStatus(409))
  }

  def mappingKnownFactsIssue(utr: Utr, arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/${utr.value}/${arn.value}/$saAgentReference"))
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
