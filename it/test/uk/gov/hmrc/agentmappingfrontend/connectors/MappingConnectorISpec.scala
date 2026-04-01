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

package uk.gov.hmrc.agentmappingfrontend.connectors

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs._

class MappingConnectorISpec
extends BaseControllerISpec {

  private val arn = Arn("ARN0001")

  private def connector = app.injector.instanceOf[MappingConnector]
  private implicit val rh: RequestHeader = FakeRequest()

  "createMapping" should {
    "create a mapping" in {
      mappingIsCreated(arn)
      await(connector.createMapping(arn)) shouldBe 201
    }

    "not create a mapping when one already exists" in {
      mappingExists(arn)
      await(connector.createMapping(arn)) shouldBe 409
    }

    "not create a mapping when there is a problem with the supplied known facts" in {
      mappingError(arn)
      await(connector.createMapping(arn)) shouldBe 403
    }
  }

  "getClientCount" should {
    "return the count" in {
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

    "return empty list if no sa mappings found for a given arn" in {
      noSaMappingsFound(arn)
      val mappings = await(connector.findSaMappingsFor(arn))
      mappings.size shouldBe 0
    }

    "throw a runtime exception when sa mappings call returns an error" in {
      mappingsError(
        arn = arn,
        regime = "sa"
      )
      intercept[RuntimeException] {
        await(connector.findSaMappingsFor(arn))
      }
    }
  }

  "delete" should {
    "delete all mappings for a given arn" in {
      mappingsDelete(arn)
      await(connector.deleteAllMappingsBy(arn)) shouldBe 204
    }
  }

}
