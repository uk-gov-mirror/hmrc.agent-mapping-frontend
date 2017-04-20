package uk.gov.hmrc.agentmappingfrontend.stubs

import uk.gov.hmrc.agentmappingfrontend.model.{Arn, Utr}
import uk.gov.hmrc.domain.SaAgentReference
import com.github.tomakehurst.wiremock.client.WireMock._

object MappingStubs {

  def mappingIsCreated(utr: Utr, arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/$utr/$arn/$saAgentReference"))
        willReturn aResponse().withStatus(201))
  }

  def mappingExists(utr: Utr, arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/$utr/$arn/$saAgentReference"))
      willReturn aResponse().withStatus(409))
  }

  def mappingKnownFactsIssue(utr: Utr, arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/$utr/$arn/$saAgentReference"))
      willReturn aResponse().withStatus(403))
  }
}
