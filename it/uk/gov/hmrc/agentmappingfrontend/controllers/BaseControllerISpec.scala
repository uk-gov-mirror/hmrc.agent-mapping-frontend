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

import akka.stream.Materializer
import com.google.inject.AbstractModule
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.{HttpFilters, NoHttpFilters}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmappingfrontend.audit.AuditService
import uk.gov.hmrc.agentmappingfrontend.support.{AuditSupport, EndpointBehaviours, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseControllerISpec extends UnitSpec with OneAppPerSuite with WireMockSupport with EndpointBehaviours with AuditSupport {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder().configure(
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.agent-mapping.port" -> wireMockPort,
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
      "authentication.login-callback.url" -> "somehost"
    )
      .overrides(new TestGuiceModule)
  }

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[AuditService]).toInstance(testAuditService)
      bind(classOf[HttpFilters]).to(classOf[NoHttpFilters])
    }
  }

  protected implicit val materializer: Materializer = app.materializer

  protected def fakeRequest(endpointMethod: String, endpointPath: String) = {
    FakeRequest(endpointMethod, endpointPath).withSession(SessionKeys.authToken -> "Bearer XYZ")
  }

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])
  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString
}
