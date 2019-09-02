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

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentmappingfrontend.model.{AuthProviderId, LegacyAgentEnrolmentType}
import uk.gov.hmrc.agentmappingfrontend.stubs.{AgentSubscriptionStubs, AuthStubs}
import uk.gov.hmrc.agentmappingfrontend.support.SubscriptionJourneyRecordSamples
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionsSpec extends BaseControllerISpec with AuthStubs with AgentSubscriptionStubs with SubscriptionJourneyRecordSamples {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]
    override def agentSubscriptionConnector: AgentSubscriptionConnector = app.injector.instanceOf[AgentSubscriptionConnector]

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest("GET", "/foo").withSession(SessionKeys.authToken -> "Bearer XYZ")

    val env = app.injector.instanceOf[Environment]
    val config = app.injector.instanceOf[Configuration]
    val appConfig = app.injector.instanceOf[AppConfig]

    def testWithAuthorisedAgent =
      await(withAuthorisedAgent("arnRefToTryAgain") { providerId => Future.successful(Ok("Done.")) })

    def testWithBasicAuth =
      await(withBasicAuth { Future.successful(Ok("Done."))})

    def testWithBasicAgentAuth =
      await(withBasicAgentAuth{Future.successful(Ok("Done."))})

    def testWithCheckForArn =
      await(withCheckForArn { optEnrolmentIdentifier => Future.successful(Ok(optEnrolmentIdentifier.toString))})

    def testWithSubscribingAgent =
      await(withSubscribingAgent("mappingArnResultId"){ agent => Future.successful(Ok("Done."))})

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

  def testAuthorisedAgentRedirectedTo(expectedLocation: String, enrolments: (String, String)*): Unit = {

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
         |  "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  }}""".stripMargin
    )

    val result: Result = TestController.testWithAuthorisedAgent
    status(result) shouldBe 303
    result.header.headers(HeaderNames.LOCATION) shouldBe expectedLocation
    ()
  }

  def testSubscribingAgentRedirectedTo(expectedLocation: String, enrolments: (String, String)*): Unit = {

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
         |  "optionalCredentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  }, "agentCode": "a1234"}""".stripMargin
    )

    val result: Result = TestController.testWithSubscribingAgent
    status(result) shouldBe 303
    result.header.headers(HeaderNames.LOCATION) shouldBe expectedLocation
    ()
  }

  "withAuthorisedAgent" should {
    "this test should cover all eligible enrolments" in {
      eligibleEnrolments.foreach { enrolment =>
        LegacyAgentEnrolmentType.exists(enrolment._1) shouldBe true
      }
      LegacyAgentEnrolmentType.foreach { t =>
        eligibleEnrolments.get(t.serviceKey).isDefined shouldBe true
      }
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
             |  "optionalCredentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        val result = TestController.testWithAuthorisedAgent
        status(result) shouldBe 200
        bodyOf(result) shouldBe "Done."
      }
    }

    "redirect to /already-linked" when {
      "agent has just a HMRC-AGENT-AGENT enrolment but not HMRC-AS-AGENT" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.alreadyMapped(id = "arnRefToTryAgain").url,
          enrolments = "HMRC-AGENT-AGENT" -> "AgentRefNumber"
        )
      }
    }

    "redirect to /not-enrolled" when {
      "agent has only 'non-agent' enrolments" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.notEnrolled(id = "arnRefToTryAgain").url,
          enrolments = "IR-SA" -> "UTR"
        )
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
             |  "optionalCredentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        val result = TestController.testWithAuthorisedAgent
        status(result) shouldBe 303
        result.header.headers(HeaderNames.LOCATION) shouldBe routes.MappingController.notEnrolled(id = "arnRefToTryAgain").url
      }

      "agent has no enrolments" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.notEnrolled(id = "arnRefToTryAgain").url,
          enrolments = ("","")
        )

      }
    }

    "redirect to /incorrect-account" when {
      "agent has just a HMRC-AS-AGENT enrolment" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.incorrectAccount(id = "arnRefToTryAgain").url,
          enrolments = "HMRC-AS-AGENT" -> "AgentReferenceNumber"
        )
      }

      "agent has both HMRC-AS-AGENT and HMRC-AGENT-AGENT enrolments" in {
        behave like testAuthorisedAgentRedirectedTo(
          expectedLocation = routes.MappingController.incorrectAccount(id = "arnRefToTryAgain").url,
          enrolments = Seq("HMRC-AS-AGENT" -> "AgentReferenceNumber", "HMRC-AGENT-AGENT" -> "AgentRefNumber") :_*
        )
      }
    }

    "redirect to sign-in if an agent is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = TestController.testWithAuthorisedAgent
      status(result) shouldBe 303
      result.header.headers(HeaderNames.LOCATION) shouldBe s"/gg/sign-in?continue=${URLEncoder.encode("somehost/foo", "utf-8")}&origin=agent-mapping-frontend"
    }
  }

  "withSubscribingAgent" should {

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
             |  "optionalCredentials": {
             |    "providerId": "12345-credId",
             |    "providerType": "GovernmentGateway"
             |  }}""".stripMargin
        )
        givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrNoContinueId)
        val result = TestController.testWithSubscribingAgent
        status(result) shouldBe 200
        bodyOf(result) shouldBe "Done."
      }
    }

    "redirect to task-list/error/incorrect-account" when {
      "agent has a HMRC-AS-AGENT enrolment" in {
        behave like testSubscribingAgentRedirectedTo(
          expectedLocation = routes.TaskListMappingController.incorrectAccount("mappingArnResultId").url,
          enrolments = "HMRC-AS-AGENT" -> "AgentRefNumber"
        )
      }
    }

    "redirect to task-list/error/already-linked" when {
      "agent has HMRC-AGENT-AGENT enrolment" in {
        behave like testSubscribingAgentRedirectedTo(
          expectedLocation = routes.TaskListMappingController.alreadyMapped("mappingArnResultId").url,
          enrolments = "HMRC-AGENT-AGENT" -> "AgentRefNumber"
        )
      }
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
      givenAuthorisedFor(s"""{"allEnrolments": []}""",s"""{}""".stripMargin)

      val result = TestController.testWithCheckForArn
      status(result) shouldBe 403
    }

    "return None no Bearer Token" in {
      givenUserIsNotAuthenticated

      val result = TestController.testWithCheckForArn
      status(result) shouldBe 200
      bodyOf(result) should include("None")
    }
  }
}
