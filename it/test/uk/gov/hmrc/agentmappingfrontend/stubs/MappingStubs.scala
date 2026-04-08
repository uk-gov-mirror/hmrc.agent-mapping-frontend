/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model.*
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn

import java.time.LocalDate

object MappingStubs:

  val listOfSaMappings = List(
    SaMapping(
      "ARN0001",
      "A12345",
      Some(LocalDate.now())
    ),
    SaMapping(
      "ARN0001",
      "B12345",
      Some(LocalDate.now())
    )
  )

  val saJsonBody = Json.toJson(SaMappings(listOfSaMappings))

  def mappingIsCreated(arn: Arn): StubMapping = stubFor(
    put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
      willReturn aResponse().withStatus(201)
  )

  def mappingExists(arn: Arn): StubMapping = stubFor(
    put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
      willReturn aResponse().withStatus(409)
  )

  def mappingError(arn: Arn): StubMapping = stubFor(
    put(urlPathEqualTo(s"/agent-mapping/mappings/arn/${arn.value}"))
      willReturn aResponse().withStatus(403)
  )

  def saMappingsFound(arn: Arn): StubMapping = stubFor(
    get(urlPathEqualTo(s"/agent-mapping/mappings/sa/${arn.value}"))
      .willReturn(aResponse().withStatus(200).withBody(saJsonBody.toString()))
  )

  def noSaMappingsFound(arn: Arn): StubMapping = stubFor(
    get(urlPathEqualTo(s"/agent-mapping/mappings/sa/${arn.value}"))
      .willReturn(aResponse().withStatus(404))
  )

  def mappingsError(
    arn: Arn,
    regime: String
  ): StubMapping = stubFor(
    get(urlPathEqualTo(s"/agent-mapping/mappings/$regime/${arn.value}"))
      .willReturn(aResponse().withStatus(500))
  )

  def mappingsDelete(arn: Arn): StubMapping = stubFor(
    delete(urlPathEqualTo(s"/agent-mapping/test-only/mappings/${arn.value}"))
      .willReturn(aResponse().withStatus(204))
  )

  def givenClientCountRecordsFound(recordCount: Int): StubMapping = stubFor(
    get(urlPathEqualTo(s"/agent-mapping/client-count"))
      .willReturn(aResponse().withStatus(200).withBody(Json.obj("clientCount" -> recordCount).toString()))
  )

  def givenGetMappingDetailsFailsForReason(
    arn: Arn,
    statusCode: Int
  ): StubMapping = stubFor(
    get(urlEqualTo(s"/agent-mapping/mappings/details/arn/${arn.value}"))
      .willReturn(aResponse().withStatus(statusCode))
  )
