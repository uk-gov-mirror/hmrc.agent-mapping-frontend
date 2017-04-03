package uk.gov.hmrc.agentmappingfrontend.stubs

import uk.gov.hmrc.agentmappingfrontend.model.Arn
import uk.gov.hmrc.domain.SaAgentReference
import com.github.tomakehurst.wiremock.client.WireMock._

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
