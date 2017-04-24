package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.SaAgentReference

object MappingStubs {

  def mappingIsCreated(arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/$arn/$saAgentReference"))
        willReturn aResponse().withStatus(201))
  }

  def mappingExists(arn: Arn, saAgentReference: SaAgentReference): Unit = {
    stubFor(put(urlPathEqualTo(s"/agent-mapping/mappings/$arn/$saAgentReference"))
      willReturn aResponse().withStatus(409))
  }
}
