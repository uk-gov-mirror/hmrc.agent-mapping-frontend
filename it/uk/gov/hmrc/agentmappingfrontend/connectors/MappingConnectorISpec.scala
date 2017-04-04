package uk.gov.hmrc.agentmappingfrontend.connectors

import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.model.Arn
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class MappingConnectorISpec extends BaseControllerISpec {
  private val arn = Arn("ARN0001")
  private val saAgentReference = SaAgentReference("ARN0001")

  private def connector = app.injector.instanceOf[MappingConnector]
  private implicit val hc = HeaderCarrier()

  "createMapping" should {
    "create a mapping" in {
      MappingStubs.mappingIsCreated(arn, saAgentReference)

      val result = await(connector.createMapping(arn, saAgentReference))

      result shouldBe ()
    }

    "not create a mapping when one already exists" in {
      MappingStubs.mappingExists(arn, saAgentReference)

      val e = intercept[Upstream4xxResponse] {
        await(connector.createMapping(arn, saAgentReference))
      }

      e.upstreamResponseCode shouldBe 409
    }
  }

}
