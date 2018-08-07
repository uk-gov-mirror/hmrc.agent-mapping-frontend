package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

object MappingStubs {

  val listOfSaMappings = List(SaMapping("ARN0001", "AgentCode1"), SaMapping("ARN0001", "AgentCode2"))

  val listOfVatMappings = List(VatMapping("ARN0001", "101747696"), VatMapping("ARN0001", "101747641"))

  val saJsonBody = Json.toJson(SaMappings(listOfSaMappings))
  val vatJsonBody = Json.toJson(VatMappings(listOfVatMappings))

  def mappingIsCreated(arn: Arn): Unit =
    stubFor(
      put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
        willReturn aResponse().withStatus(201))

  def mappingExists(arn: Arn): Unit =
    stubFor(
      put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
        willReturn aResponse().withStatus(409))

  def mappingKnownFactsIssue(arn: Arn): Unit =
    stubFor(
      put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
        willReturn aResponse().withStatus(403))

  def saMappingsFound(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/sa/${arn.value}"))
        .willReturn(aResponse().withStatus(200).withBody(saJsonBody.toString())))

  def vatMappingsFound(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/vat/${arn.value}"))
        .willReturn(aResponse().withStatus(200).withBody(vatJsonBody.toString())))

  def noSaMappingsFound(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/sa/${arn.value}"))
        .willReturn(aResponse().withStatus(404)))

  def noVatMappingsFound(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/vat/${arn.value}"))
        .willReturn(aResponse().withStatus(404)))

  def mappingsDelete(arn: Arn): Unit =
    stubFor(
      delete(urlPathEqualTo(s"/agent-mapping/test-only/mappings/${arn.value}"))
        .willReturn(aResponse().withStatus(204)))
}
