package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

object MappingStubs {

  val listOfSaMappings = List(SaMapping("ARN0001", "AgentCode1"), SaMapping("ARN0001", "AgentCode2"))

  val listOfVatMappings = List(VatMapping("ARN0001", "101747696"), VatMapping("ARN0001", "101747641"))

  val saJsonBody = Json.toJson(SaMappings(listOfSaMappings))
  val vatJsonBody = Json.toJson(VatMappings(listOfVatMappings))

  def mappingIsCreated(arn: Arn): StubMapping =
    stubFor(
      put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
        willReturn aResponse().withStatus(201))

  def mappingExists(arn: Arn): StubMapping =
    stubFor(
      put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
        willReturn aResponse().withStatus(409))

  def mappingKnownFactsIssue(arn: Arn): StubMapping =
    stubFor(
      put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
        willReturn aResponse().withStatus(403))

  def saMappingsFound(arn: Arn): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/sa/${arn.value}"))
        .willReturn(aResponse().withStatus(200).withBody(saJsonBody.toString())))

  def vatMappingsFound(arn: Arn): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/vat/${arn.value}"))
        .willReturn(aResponse().withStatus(200).withBody(vatJsonBody.toString())))

  def noSaMappingsFound(arn: Arn): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/sa/${arn.value}"))
        .willReturn(aResponse().withStatus(404)))

  def noVatMappingsFound(arn: Arn): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/mappings/vat/${arn.value}"))
        .willReturn(aResponse().withStatus(404)))

  def mappingsDelete(arn: Arn): StubMapping =
    stubFor(
      delete(urlPathEqualTo(s"/agent-mapping/test-only/mappings/${arn.value}"))
        .willReturn(aResponse().withStatus(204)))

  def givenClientCountRecordsFound(recordCount: Int): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/client-count"))
        .willReturn(aResponse().withStatus(200).withBody(Json.obj("clientCount" -> recordCount).toString()))
    )

  def getClientCount500(): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-mapping/client-count"))
        .willReturn(aResponse().withStatus(500))
    )

  def mappingDetailsAreCreated(arn: Arn, mappingDetailsRequest: MappingDetailsRequest): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/agent-mapping/mappings/details/arn/${arn.value}"))
        .withRequestBody(equalToJson(Json.toJson(mappingDetailsRequest).toString()))
        .willReturn(aResponse().withStatus(Status.CREATED))
    )

  def mappingDetailsCreationFails(arn: Arn, mappingDetailsRequest: MappingDetailsRequest): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/agent-mapping/mappings/details/arn/${arn.value}"))
        .withRequestBody(equalToJson(Json.toJson(mappingDetailsRequest).toString()))
        .willReturn(aResponse().withStatus(Status.CONFLICT))
    )

  def givenMappingDetailsExistFor(arn: Arn, mappingDetailsRepositoryRecord: MappingDetailsRepositoryRecord): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-mapping/mappings/details/arn/${arn.value}"))
        .willReturn(aResponse().withStatus(Status.OK).withBody(Json.toJson(mappingDetailsRepositoryRecord).toString()))
    )

  def givenGetMappingDetailsFailsForReason(arn: Arn, statusCode: Int): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-mapping/mappings/details/arn/${arn.value}"))
        .willReturn(aResponse().withStatus(statusCode))
    )
}
