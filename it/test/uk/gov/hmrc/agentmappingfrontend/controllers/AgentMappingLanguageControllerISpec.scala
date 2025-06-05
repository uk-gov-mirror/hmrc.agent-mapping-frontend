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
import play.api.test.Helpers.cookies
import play.api.test.Helpers

import scala.concurrent.duration._

class AgentMappingLanguageControllerISpec
extends BaseControllerISpec {

  implicit val timeout: FiniteDuration = 2.second

  private lazy val controller: AgentMappingLanguageController = app.injector.instanceOf[AgentMappingLanguageController]

  "GET /language/:lang" should {

    val request = FakeRequest("GET", "/language/english")

    "redirect to https://www.tax.service.gov.uk/agent-mapping when the request header contains no referer" in {

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      Helpers.redirectLocation(result)(timeout) shouldBe Some("https://www.tax.service.gov.uk/agent-mapping")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"
    }

    "redirect to /some-page when the request header contains referer /some-page" in {

      val request = FakeRequest("GET", "/language/english").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      Helpers.redirectLocation(result)(timeout) shouldBe Some("/some-page")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"
    }

    "redirect to /some-page with lang set to 'cy' when the user has selected Welsh" in {

      val request = FakeRequest("GET", "/language/cymraeg").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("cymraeg")(request)
      status(result) shouldBe 303
      Helpers.redirectLocation(result)(timeout) shouldBe Some("/some-page")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "cy"
    }
  }

}
