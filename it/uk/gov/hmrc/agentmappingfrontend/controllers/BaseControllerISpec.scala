/*
 * Copyright 2017 HM Revenue & Customs
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

import com.google.inject.AbstractModule
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.{contentType, _}
import uk.gov.hmrc.agentmappingfrontend.support.WireMockSupport
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseControllerISpec extends UnitSpec with OneAppPerSuite with WireMockSupport {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder().configure(
        "microservice.services.auth.port" -> wireMockPort
      ).overrides(new TestGuiceModule)
  }

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
    }
  }

  protected implicit val materializer = app.materializer

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstrings: String*): Unit = {
    status(result) shouldBe OK
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    expectedSubstrings.foreach(s => bodyOf(result) should include(s))
  }
}

