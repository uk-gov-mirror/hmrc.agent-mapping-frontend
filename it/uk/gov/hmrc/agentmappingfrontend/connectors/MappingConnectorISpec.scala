package uk.gov.hmrc.agentmappingfrontend.connectors

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs._
import uk.gov.hmrc.agentmappingfrontend.support.MetricTestSupport
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.http.HeaderCarrier

class MappingConnectorISpec extends BaseControllerISpec with MetricTestSupport {
  private val arn = Arn("ARN0001")

  private def connector = app.injector.instanceOf[MappingConnector]
  private implicit val hc = HeaderCarrier()

  "createMapping" should {
    "create a mapping" in {
      givenCleanMetricRegistry()
      mappingIsCreated(arn)
      await(connector.createMapping(arn)) shouldBe 201
      timerShouldExistsAndBeenUpdated("ConsumedAPI-Mapping-CreateMapping-PUT")
    }

    "not create a mapping when one already exists" in {
      mappingExists(arn)
      await(connector.createMapping(arn)) shouldBe 409
    }

    "not create a mapping when there is a problem with the supplied known facts" in {
      mappingKnownFactsIssue(arn)
      await(connector.createMapping(arn)) shouldBe 403
    }
  }

  "find" should {
    "find all sa mappings for a given arn" in {
      saMappingsFound(arn)
      val mappings = await(connector.findSaMappingsFor(arn))

      mappings.size shouldBe 2
      mappings.head.arn shouldBe arn.value
    }

    "find all vat mappings for a given arn" in {
      vatMappingsFound(arn)
      val mappings = await(connector.findVatMappingsFor(arn))

      mappings.size shouldBe 2
      mappings.head.arn shouldBe arn.value
    }

    "return empty list if no sa mappings found for a given arn" in {
      noSaMappingsFound(arn)
      val mappings = await(connector.findSaMappingsFor(arn))
      mappings.size shouldBe 0
    }

    "return empty list if no vat mappings found for a given arn" in {
      noVatMappingsFound(arn)
      val mappings = await(connector.findVatMappingsFor(arn))
      mappings.size shouldBe 0
    }
  }

  "delete" should {
    "delete all mappings for a given arn" in {
      mappingsDelete(arn)
      await(connector.deleteAllMappingsBy(arn)) shouldBe 204
    }

  }
}
