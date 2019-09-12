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
import org.jsoup.Jsoup
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HttpFilters, NoHttpFilters}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmappingfrontend.support.{EndpointBehaviours, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._

abstract class BaseControllerISpec
    extends UnitSpec with GuiceOneAppPerSuite with WireMockSupport with EndpointBehaviours {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"          -> wireMockPort,
        "microservice.services.agent-mapping.port" -> wireMockPort,
        "microservice.services.agent-subscription.port" -> wireMockPort,
        "application.router"                       -> "testOnlyDoNotUseInAppConf.Routes",
        "authentication.login-callback.url"        -> "somehost",
        "clientCount.maxRecords" -> 15
      )
      .overrides(new TestGuiceModule)

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[HttpFilters]).to(classOf[NoHttpFilters])
      bind(classOf[AuditConnector]).toInstance(new AuditConnector {
        override def auditingConfig: AuditingConfig = AuditingConfig(None, enabled = false, auditSource = "agent-mapping")
      })
    }
  }

  protected implicit val materializer: Materializer = app.materializer

  protected def fakeRequest(endpointMethod: String, endpointPath: String) =
    FakeRequest(endpointMethod, endpointPath).withSession(SessionKeys.authToken -> "Bearer XYZ")

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])
  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString
  protected def htmlEscapedMessage(key: String, args: Any*): String =
    HtmlFormat.escape(Messages(key, args: _*)).toString

  protected def checkHtmlResultContainsEscapedMsgs(result: Result, expectedMessageKeys: String*): Unit = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")

    expectedMessageKeys.foreach { messageKey =>
      withClue(s"Expected message key '$messageKey' to exist: ") {
        Messages.isDefinedAt(messageKey) shouldBe true
      }

      val expectedContent = Messages(messageKey)
      withClue(s"Expected content ('$expectedContent') for message key '$messageKey' to be in request body: ") {
        bodyOf(result) should include(htmlEscapedMessage(expectedContent))
      }
    }
  }

  protected def checkHtmlResultContainsMsgs(result: Result, expectedMessageKeys: String*): Unit = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")

    expectedMessageKeys.foreach { messageKey =>
      withClue(s"Expected message key '$messageKey' to exist: ") {
        Messages.isDefinedAt(messageKey) shouldBe true
      }

      val expectedContent = Messages(messageKey)
      withClue(s"Expected content ('$expectedContent') for message key '$messageKey' to be in request body: ") {
        bodyOf(result) should include(expectedContent)
      }
    }
  }

  protected def checkIsHtml200(result: Result) = {
    status(result) shouldBe 200
    charset(result) shouldBe Some("utf-8")
    contentType(result) shouldBe Some("text/html")
  }

  protected def containSubstrings(expectedSubstrings: String*): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        checkIsHtml200(result)

        val resultBody = bodyOf(result)
        val (strsPresent, strsMissing) = expectedSubstrings.partition { expectedSubstring =>
          expectedSubstring.trim should not be ""
          resultBody.contains(expectedSubstring)
        }

        MatchResult(
          strsMissing.isEmpty,
          s"Expected substrings are missing in the response: ${strsMissing.mkString("\"", "\", \"", "\"")}",
          s"Expected substrings are present in the response : ${strsPresent.mkString("\"", "\", \"", "\"")}"
        )
      }
    }

  protected def checkMessageIsDefined(messageKey: String) =
    withClue(s"Message key ($messageKey) should be defined: ") {
      Messages.isDefinedAt(messageKey) shouldBe true
    }

  protected def containSubmitButton(
                                     expectedMessageKey: String,
                                     expectedElementId: String,
                                     expectedTagName: String = "button",
                                     expectedType: String = "submit"): Matcher[Result] = {
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))

        checkMessageIsDefined(expectedMessageKey)

        val foundElement = doc.getElementById(expectedElementId)

        val isAsExpected = Option(foundElement) match {
          case None => false
          case Some(elAmls) => {
            val isExpectedTag = elAmls.tagName() == expectedTagName
            val isExpectedType = elAmls.attr("type") == expectedType
            val hasExpectedMsg = elAmls.text() == htmlEscapedMessage(expectedMessageKey)
            isExpectedTag && isExpectedType && hasExpectedMsg
          }
        }

        MatchResult(
          isAsExpected,
          s"""Response does not contain a submit button with id "$expectedElementId" and type "$expectedType" with content for message key "$expectedMessageKey" """,
          s"""Response contains a submit button with id "$expectedElementId" and type "$expectedType" with content for message key "$expectedMessageKey" """
        )
      }
    }
  }

  protected def containLink(expectedMessageKey: String, expectedHref: String): Matcher[Result] = {
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))
        checkMessageIsDefined(expectedMessageKey)
        val foundElement = doc.select(s"a[href=$expectedHref]").first()
        val wasFoundWithCorrectMessage = Option(foundElement) match {
          case None => false
          case Some(element) => element.text() == htmlEscapedMessage(expectedMessageKey)
        }
        MatchResult(
          wasFoundWithCorrectMessage,
          s"""Response does not contain a link to "$expectedHref" with content for message key "$expectedMessageKey" """,
          s"""Response contains a link to "$expectedHref" with content for message key "$expectedMessageKey" """
        )
      }
    }
  }
}
