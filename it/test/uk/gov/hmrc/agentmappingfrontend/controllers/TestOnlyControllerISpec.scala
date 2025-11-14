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

package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.testOnly.TestOnlyController
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs._
import play.api.test.Helpers._

class TestOnlyControllerISpec
extends BaseControllerISpec {

  private lazy val controller: TestOnlyController = app.injector.instanceOf[TestOnlyController]

  val arn = Arn("ARN0001")
  val sa = "AgentCode1"

  "findMappings" should {
    "return OK with list of sa mappings" in {
      saMappingsFound(arn)
      val response = await(controller.findSaMappings(arn)(FakeRequest()))
      status(response) shouldBe 200
      val html = bodyOf(response)
      html should include(arn.value)
      html should include(sa)
    }

    "return Not Found when given arn does not have any sa mappings" in {
      noSaMappingsFound(arn)
      val response = await(controller.findSaMappings(arn)(FakeRequest()))

      status(response) shouldBe 404
      bodyOf(response) should include(s"No mappings found for ${arn.value}")
    }
  }

  "delete" should {
    "return OK when given Arn to delete mapping" in {
      mappingsDelete(arn)
      val response = await(controller.deleteAllMappings(arn)(FakeRequest()))
      status(response) shouldBe 200
      val html = bodyOf(response)
      html should include(arn.value)
    }
  }

}
