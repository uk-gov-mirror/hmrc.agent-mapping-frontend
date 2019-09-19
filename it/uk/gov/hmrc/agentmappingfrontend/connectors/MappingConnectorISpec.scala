package uk.gov.hmrc.agentmappingfrontend.connectors

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.http.Status
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.model.{AuthProviderId, MappingDetails, MappingDetailsRepositoryRecord, MappingDetailsRequest}
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs._
import uk.gov.hmrc.agentmappingfrontend.support.MetricTestSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

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

  "getClientCount" should {
    "return the count" in {
      givenCleanMetricRegistry()
      givenClientCountRecordsFound(299)
      await(connector.getClientCount) shouldBe 299
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

  "createOrUpdateMappingDetails" should {
    "create mapping details successfully" in {
      val mappingDetailsRequest = MappingDetailsRequest(AuthProviderId("cred-1234"), "1234", 5)
      mappingDetailsAreCreated(arn, mappingDetailsRequest)
      await(connector.createOrUpdateMappingDetails(arn, mappingDetailsRequest)) shouldBe Status.CREATED
    }
    "creation of mapping fails throw a RuntimeException" in {
      val mappingDetailsRequest = MappingDetailsRequest(AuthProviderId("cred-1234"), "1234", 5)
      mappingDetailsCreationFails(arn, mappingDetailsRequest)
      intercept[RuntimeException] {
        await(connector.createOrUpdateMappingDetails(arn, mappingDetailsRequest))
      }
    }
  }

  "getMappingDetails" should {
    val dateTime = LocalDateTime.parse("2019-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val mappingDetailsRepositoryRecord = MappingDetailsRepositoryRecord(Arn("TARN0000001"), Seq(MappingDetails(AuthProviderId("cred-1234"), "1234", 5, dateTime)))
    "retrieve the mapping details" in {
      givenMappingDetailsExistFor(arn, mappingDetailsRepositoryRecord)
      await(connector.getMappingDetails(arn)) shouldBe Some(mappingDetailsRepositoryRecord)
    }

    "return None when there are no mapping details" in {
      givenGetMappingDetailsFailsForReason(arn, 404)
      await(connector.getMappingDetails(arn)) shouldBe None
    }

    "return None when there is some exception" in {
      givenGetMappingDetailsFailsForReason(arn, 404)
      await(connector.getMappingDetails(arn)) shouldBe None
    }
  }
}
