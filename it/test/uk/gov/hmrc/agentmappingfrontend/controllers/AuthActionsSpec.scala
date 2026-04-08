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

import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Result
import play.api.mvc.Results.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Configuration
import play.api.Environment
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.model.LegacyAgentEnrolmentType
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class AuthActionsSpec
extends BaseControllerISpec
with AuthStubs:

  object TestController
  extends AuthActions:

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/foo")
      .withSession(SessionKeys.authToken -> "Bearer XYZ")

    val env: Environment = app.injector.instanceOf[Environment]
    lazy val config: Configuration = app.injector.instanceOf[Configuration]

    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    def testWithAuthorisedSaAgent: Result = await(withAuthorisedSaAgent("arnRefToTryAgain")(_ => Future.successful(Ok("Done."))))

    def testWithBasicAuth: Result = await(withBasicAuth(Future.successful(Ok("Done."))))

    def testWithBasicAgentAuth: Result = await(withBasicAgentAuth(Future.successful(Ok("Done."))))

    def testWithCheckForArn: Result = await(withCheckForArn(optEnrolmentIdentifier => Future.successful(Ok(optEnrolmentIdentifier.toString))))

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

  def testAuthorisedAgentRedirectedTo(
    expectedLocation: String,
    enrolments: (String, String)*
  ): Unit =

    val enrolmentsArr = enrolments
      .map { case (key, identifier) =>
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
      }
      .mkString("[", ", ", "]")

    givenAuthorisedFor(
      "{}",
      s"""{
         |  "allEnrolments": $enrolmentsArr,
         |  "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  },
         |  "affinityGroup": "Agent"
         |}""".stripMargin
    )

    val result: Result = TestController.testWithAuthorisedSaAgent
    status(result) shouldBe 303
    result.header.headers(HeaderNames.LOCATION) shouldBe expectedLocation
    ()

  "withAuthorisedAgent" should {
    "this test should cover all eligible enrolments" in {
      eligibleEnrolments.foreach { enrolment =>
        LegacyAgentEnrolmentType.exists(enrolment._1) shouldBe true
      }
      LegacyAgentEnrolmentType.foreach { t =>
        eligibleEnrolments.contains(t.serviceKey) shouldBe true
      }
    }

    "check if agent is enrolled for the eligible enrolment IR-SA-AGENT and extract IRAgentReference" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |  "allEnrolments": [
           |    { "key":"IR-SA-AGENT", "identifiers": [
           |      { "key":"IRAgentReference", "value": "fooReference" }
           |    ]}
           |  ],
           |  "optionalCredentials": {
           |    "providerId": "12345-credId",
           |    "providerType": "GovernmentGateway"
           |  },
           |  "affinityGroup": "Agent"
           |  }""".stripMargin
      )
      val result = TestController.testWithAuthorisedSaAgent
      status(result) shouldBe 200
      bodyOf(result) shouldBe "Done."
    }

    "redirect to /problem-with-details" when {
      "agent has just a HMRC-AGENT-AGENT enrolment but not HMRC-AS-AGENT" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.problemWithDetails(id = "arnRefToTryAgain").url,
          enrolments = "HMRC-AGENT-AGENT" -> "AgentRefNumber"
        )
      }
      "agent does not have IR-SA-AGENT enrolment" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.problemWithDetails(id = "arnRefToTryAgain").url,
          enrolments = ("", "")
        )
      }
      "agent has only inactive IR-SA-AGENT enrolment" in {
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
             |  "optionalCredentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  },
             |  "affinityGroup": "Agent"
             |  }""".stripMargin
        )
        val result = TestController.testWithAuthorisedSaAgent
        status(result) shouldBe 303
        result.header
          .headers(HeaderNames.LOCATION) shouldBe routes.MappingController.problemWithDetails(id = "arnRefToTryAgain").url
      }
    }

    "redirect to /wrong-sign-in-asa" when {
      "agent has a HMRC-AS-AGENT enrolment" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.wrongSignInDetailsAsa(id = "arnRefToTryAgain").url,
          enrolments = "HMRC-AS-AGENT" -> "AgentReferenceNumber"
        )
      }
    }

    "redirect to /wrong-sign-in-not-agent" when {
      "agent signed in with a personal cred" in {
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
             |  "optionalCredentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  },
             |  "affinityGroup": "Individual"
             |  }""".stripMargin
        )
        val result = TestController.testWithAuthorisedSaAgent
        status(result) shouldBe 303
        result.header
          .headers(HeaderNames.LOCATION) shouldBe routes.MappingController.wrongSignInDetailsNotAgent(id = "arnRefToTryAgain").url
      }
    }

    "redirect to sign-in if an agent is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = TestController.testWithAuthorisedSaAgent
      status(result) shouldBe 303
      result.header.headers(
        HeaderNames.LOCATION
      ) shouldBe s"http://localhost:9099/bas-gateway/sign-in?continue_url=http://localhost:9438/foo&origin=agent-mapping-frontend"
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
      result.header.headers(
        HeaderNames.LOCATION
      ) shouldBe s"http://localhost:9099/bas-gateway/sign-in?continue_url=http://localhost:9438/foo&origin=agent-mapping-frontend"
    }
  }

  "withBasicAgentAuth" should {
    "check if the user is logged in" in {
      givenAuthorisedFor("{}", s"""{}""".stripMargin)
      val result = TestController.testWithBasicAgentAuth
      status(result) shouldBe 200
      bodyOf(result) shouldBe "Done."
    }

    "redirect to sign-in if a user is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = TestController.testWithBasicAgentAuth
      status(result) shouldBe 303
      result.header.headers(
        HeaderNames.LOCATION
      ) shouldBe s"http://localhost:9099/bas-gateway/sign-in?continue_url=http://localhost:9438/foo&origin=agent-mapping-frontend"
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
           |  "optionalCredentials": {
           |    "providerId": "12345-credId",
           |    "providerType": "GovernmentGateway"
           |  }}""".stripMargin
      )
      val result = TestController.testWithCheckForArn
      status(result) shouldBe 200
      bodyOf(result) should include("Some(Arn(TARN0000001)")
    }

    "return None when user has no HMRC-AS-AGENT enrolment" in {
      givenAuthorisedFor(s"""{"allEnrolments": []}""", s"""{}""".stripMargin)

      val result = TestController.testWithCheckForArn
      status(result) shouldBe 403
    }

    "return None no Bearer Token" in {
      givenUserIsNotAuthenticated()

      val result = TestController.testWithCheckForArn
      status(result) shouldBe 200
      bodyOf(result) should include("None")
    }
  }
