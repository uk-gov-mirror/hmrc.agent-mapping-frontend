package uk.gov.hmrc.agentmappingfrontend.connectors

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.model.{Arn, Utr}
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.{mappingExists, mappingIsCreated}
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}

class MappingConnectorISpec extends BaseControllerISpec {
  private val arn = Arn("ARN0001")
  private val saAgentReference = SaAgentReference("ARN0001")
  private val utr = Utr("2000000000")

  private def connector = app.injector.instanceOf[MappingConnector]
  private implicit val hc = HeaderCarrier()

  "createMapping" should {
    "create a mapping" in {
      mappingIsCreated(utr, arn, saAgentReference)

      val result = await(connector.createMapping(utr, arn, saAgentReference))

      result shouldBe ()
    }

    "not create a mapping when one already exists" in {
      mappingExists(utr, arn, saAgentReference)

      val e = intercept[Upstream4xxResponse] {
        await(connector.createMapping(utr, arn, saAgentReference))
      }

      e.upstreamResponseCode shouldBe 409
    }
  }

}
