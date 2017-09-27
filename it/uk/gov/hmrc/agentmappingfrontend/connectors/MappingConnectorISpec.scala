package uk.gov.hmrc.agentmappingfrontend.connectors

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.HeaderCarrier

class MappingConnectorISpec extends BaseControllerISpec {
  private val arn = Arn("ARN0001")
  private val saAgentReference = SaAgentReference("ARN0001")
  private val utr = Utr("2000000000")

  private def connector = app.injector.instanceOf[MappingConnector]
  private implicit val hc = HeaderCarrier()

  "createMapping" should {
    "create a mapping" in {
      mappingIsCreated(utr, arn, saAgentReference)
      await(connector.createMapping(utr, arn, saAgentReference)) shouldBe 201
    }

    "not create a mapping when one already exists" in {
      mappingExists(utr, arn, saAgentReference)
      await(connector.createMapping(utr, arn, saAgentReference)) shouldBe 409
    }

    "not create a mapping when there is a problem with the supplied known facts" in {
      mappingKnownFactsIssue(utr, arn, saAgentReference)
      await(connector.createMapping(utr, arn, saAgentReference)) shouldBe 403
    }
  }

  "find" should {
    "find all mappings for a given arn" in {
      mappingsFound(arn)
      val mappings = await(connector.find(arn))

      mappings.size shouldBe 2
      mappings.head.arn shouldBe arn.value
    }

    "return empty list if no mappings found for a given arn" in {
      noMappingsFound(arn)
      val mappings = await(connector.find(arn))
      mappings.size shouldBe 0
    }

  }

  "delete" should {
    "delete all mappings for a given arn" in {
      mappingsFound(arn)
      val mappings = await(connector.find(arn))
      mappings.size shouldBe 2

      mappingsDelete(arn)
      val deleteMappings = await(connector.delete(arn))
      deleteMappings shouldBe 200
    }

  }
}
