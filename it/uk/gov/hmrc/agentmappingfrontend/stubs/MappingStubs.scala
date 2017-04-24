package uk.gov.hmrc.agentmappingfrontend.stubs

import uk.gov.hmrc.domain.SaAgentReference
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

object MappingStubs {

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
}
