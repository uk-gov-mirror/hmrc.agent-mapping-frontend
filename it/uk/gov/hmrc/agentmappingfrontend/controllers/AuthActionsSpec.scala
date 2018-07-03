/*
 * Copyright 2018 HM Revenue & Customs
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

import java.net.URLEncoder

import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentmappingfrontend.auth.{Auth, AuthActions}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionsSpec extends BaseControllerISpec with AuthStubs {

  object TestController extends AuthActions {
    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest("GET", "/foo").withSession(SessionKeys.authToken -> "Bearer XYZ")

    val env = app.injector.instanceOf[Environment]
    val config = app.injector.instanceOf[Configuration]
    val appConfig = app.injector.instanceOf[AppConfig]

    def testWithAuthorisedAgent =
      await(withAuthorisedAgent { providerId => Future.successful(Ok("Done.")) })

    def testWithBasicAuth =
      await(withBasicAuth { Future.successful(Ok("Done."))})

    def testWithCheckForArn =
      await(withCheckForArn { optEnrolmentIdentifier => Future.successful(Ok(optEnrolmentIdentifier.toString))})
  }

  private val eligibleEnrolments = Map(
    "IR-SA-AGENT" -> "IRAgentReference",
    "HMCE-VAT-AGNT" -> "AgentRefNo",
    "HMRC-CHAR-AGENT" -> "AGENTCHARID",
    "HMRC-GTS-AGNT" -> "HMRCGTSAGENTREF",
    "HMRC-MGD-AGNT" -> "HMRCMGDAGENTREF",
    "HMRC-NOVRN-AGNT" -> "VATAgentRefNo",
    "IR-CT-AGENT" -> "IRAgentReference",
    "IR-PAYE-AGENT" -> "IRAgentReference",
    "IR-SDLT-AGENT" -> "STORN"
  )

  "withAuthorisedAgent" should {
    "this test should cover all eligible enrolments" in {
      Auth.validEnrolments.forall(eligibleEnrolments.contains) shouldBe true
    }

    eligibleEnrolments.foreach { case (enrolment, identifier) =>
      s"check if agent is enrolled for the eligible enrolment $enrolment and extract $identifier" in {
        givenAuthorisedFor(
          "{}",
          s"""{
             |  "allEnrolments": [
             |    { "key":"$enrolment", "identifiers": [
             |      { "key":"$identifier", "value": "fooReference" }
             |    ]}
             |  ],
             |  "credentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        val result = TestController.testWithAuthorisedAgent
        status(result) shouldBe 200
        bodyOf(result) shouldBe "Done."
      }
    }

    "redirect to /already-mapped" when {
      def testRedirectToAlreadyMapped(enrolments: (String, String)*): Unit = {

        val enrolmentsArr = enrolments.map { case (key, identifier) =>
          s"""
             |{
             |  "key":"$key",
             |  "identifiers": [
             |    {
             |      "key":"$identifier",
             |      "value": "TARN0000001"
             |    }
             |  ]
             |}
             """.stripMargin
        }.mkString("[", ", ", "]")

        givenAuthorisedFor(
          "{}",
          s"""{
             |  "allEnrolments": $enrolmentsArr,
             |  "credentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        val result = TestController.testWithAuthorisedAgent
        status(result) shouldBe 303
        result.header.headers(HeaderNames.LOCATION) shouldBe routes.MappingController.alreadyMapped().url
      }

      "agent has just a HMRC-AS-AGENT enrolment" in {
        behave like testRedirectToAlreadyMapped(("HMRC-AS-AGENT", "AgentReferenceNumber"))
      }

      "agent has just a HMRC-AGENT-AGENT enrolment" in {
        behave like testRedirectToAlreadyMapped(("HMRC-AGENT-AGENT", "AgentRefNumber"))
      }

      "agent has both HMRC-AS-AGENT and HMRC-AGENT-AGENT enrolments" in {
        behave like testRedirectToAlreadyMapped(("HMRC-AS-AGENT", "AgentReferenceNumber"), ("HMRC-AGENT-AGENT", "AgentRefNumber"))
      }
    }

    "redirect to /not-enrolled" when {
      "agent has only 'non-agent' enrolments" in {
        givenAuthorisedFor(
          "{}",
          s"""{
             |  "allEnrolments": [
             |    { "key":"IR-SA", "identifiers": [
             |     { "key":"UTR", "value": "fooReference" }
             |    ]}
             |  ],
             |  "credentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        val result = TestController.testWithAuthorisedAgent
        status(result) shouldBe 303
        result.header.headers(HeaderNames.LOCATION) shouldBe routes.MappingController.notEnrolled().url
      }

      "agent has only inactive (but otherwise eligible) enrolments" in {
        givenAuthorisedFor(
          "{}",
          s"""{
             |  "allEnrolments": [
             |    {
             |      "key":"IR-SA-AGENT",
             |      "identifiers": [ { "key":"IRAgentReference", "value": "fooReference" } ],
             |      "state": "Inactive"
             |    }
             |  ],
             |  "credentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        val result = TestController.testWithAuthorisedAgent
        status(result) shouldBe 303
        result.header.headers(HeaderNames.LOCATION) shouldBe routes.MappingController.notEnrolled().url
      }

      "agent has no enrolments" in {
        givenAuthorisedFor(
          "{}",
          s"""{
             |  "allEnrolments": [],
             |  "credentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        val result = TestController.testWithAuthorisedAgent
        status(result) shouldBe 303
        result.header.headers(HeaderNames.LOCATION) shouldBe routes.MappingController.notEnrolled().url
      }
    }

    "redirect to sign-in if an agent is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = TestController.testWithAuthorisedAgent
      status(result) shouldBe 303
      result.header.headers(HeaderNames.LOCATION) shouldBe s"/gg/sign-in?continue=${URLEncoder.encode("somehost/foo", "utf-8")}&origin=agent-mapping-frontend"
    }
  }

  "withBasicAuth" should {
    "check if the user is logged in" in {
      givenAuthorisedFor("{}", s"""{}""".stripMargin)
      val result = TestController.testWithBasicAuth
      status(result) shouldBe 200
      bodyOf(result) shouldBe "Done."
    }

    "redirect to sign-in if a user is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = TestController.testWithBasicAuth
      status(result) shouldBe 303
      result.header.headers(HeaderNames.LOCATION) shouldBe s"/gg/sign-in?continue=${URLEncoder.encode("somehost/foo", "utf-8")}&origin=agent-mapping-frontend"
    }
  }

  "withCheckForArn - extract HMRC-AS-AGENT EnrolmentIdentifier" should {
    "return EnrolmentIdentifier if user has HMRC-AS-AGENT enrolment" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |  "allEnrolments": [
           |    {
           |      "key":"HMRC-AS-AGENT",
           |      "identifiers": [ { "key":"AgentReferenceNumber", "value": "TARN0000001" } ],
           |      "state": "active"
           |    }
           |  ],
           |  "credentials": {
           |    "providerId": "12345-credId",
           |    "providerType": "GovernmentGateway"
           |  }}""".stripMargin
      )
      val result = TestController.testWithCheckForArn
      status(result) shouldBe 200
      bodyOf(result) should include("Some(EnrolmentIdentifier(AgentReferenceNumber,TARN0000001))")
    }

    "return None when user has no HMRC-AS-AGENT enrolment" in {
      givenAuthorisedFor("{}",s"""{}""".stripMargin)

      val result = TestController.testWithCheckForArn
      status(result) shouldBe 200
      bodyOf(result) should include("None")
    }

    "return None no Bearer Token" in {
      givenUserIsNotAuthenticated

      val result = TestController.testWithCheckForArn
      status(result) shouldBe 200
      bodyOf(result) should include("None")
    }
  }
}
