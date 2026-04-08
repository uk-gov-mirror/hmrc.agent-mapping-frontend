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

import com.google.inject.AbstractModule
import play.api.http.Writeable
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.agentmappingfrontend.model.*
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnResult
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.*
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.*
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import org.mongodb.scala.ObservableFuture

class MappingControllerISpec
extends BaseControllerISpec
with AuthStubs
with MongoSupport:

  override def additionalConfig: Map[String, String] = Map("mongodb.uri" -> mongoUri)

  override def moduleWithOverrides: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[MongoComponent]).toInstance(mongoComponent)

  val arn: Arn = Arn("TARN0000001")

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "context root" should {
    "redirect to the start page" in {
      val request = fakeRequest(GET, "/agent-mapping/")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).head should include("/start")
    }
  }

  "GET /start" should {
    "show start page with 200 if user has authenticated HMRC-AS-AGENT" in {
      givenUserIsAuthenticated(mtdAsAgent)
      saMappingsFound(arn)
      val request = FakeRequest(GET, "/agent-mapping/start").withSession(SessionKeys.authToken -> "Bearer XYZ")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "start.li.header",
        "start.p1",
        "start.inset",
        "start.li.header",
        "start.li1",
        "start.li2",
        "start.details.link",
        "start.details1",
        "start.details4",
        "authorisationsAdded.table.agentReference",
        "authorisationsAdded.table.dateCreated",
        "start.button.agent.code"
      )
      bodyOf(result) should include("/agent-services-account") // default backlink
    }

    "show start page with 200 if user has no previous mapping" in {
      givenUserIsAuthenticated(mtdAsAgent)
      noSaMappingsFound(arn)
      val request = FakeRequest(GET, "/agent-mapping/start").withSession(SessionKeys.authToken -> "Bearer XYZ")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "start.li.header",
        "start.p1",
        "start.inset",
        "start.li.header",
        "start.li1",
        "start.li2",
        "start.details.link",
        "start.details1",
        "start.details2",
        "start.details3",
        "start.button.agent.code"
      )

      bodyOf(result) should include("/agent-services-account") // default backlink
    }

    "get the backLink from the request.session OriginForMapping key" in {
      givenUserIsAuthenticated(mtdAsAgent)
      noSaMappingsFound(arn)
      val request = fakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include("/invitations/foo")
    }

    "303 the /sign-in-required for unAuthenticated" in {
      givenUserIsNotAuthenticated()
      val request = fakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      redirectLocation(result) shouldBe Some(routes.MappingController.needAgentServicesAccount.url)
    }

    "303 to /sign-in-required when user without HMRC-AS-AGENT/ARN" in {
      givenAuthorisedFor("notHMRCASAGENT")
      val request = fakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.needAgentServicesAccount.url)
    }
  }

  "/sign-in-required" should {
    "200 the /start/sign-in-required page when not logged in" in {
      givenUserIsNotAuthenticated()
      val request = fakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "start.not-signed-in.title")
    }

    "200 the /sign-in-required page as NO ARN is found" in {
      givenAuthorisedFor("notHMRCASAGENT")
      val request = fakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "start.not-signed-in.title",
        "button.signIn"
      )
    }

    "303 the /start page when user has HMRC-AS-AGENT/ARN and 'Sign in with another account' button holds idReference to agent's ARN" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = fakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      redirectLocation(result) shouldBe Some(routes.MappingController.start.url)
    }
  }

  "GET /agent-code" when {
    "the agent is on a mapping journey" should {
      "show the agent code page when record is found" in {
        val testData = MappingArnResult(
          arn = arn,
          agentCode = None
        )
        await(repo.collection.insertOne(testData).toFuture())
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showAgentCode(testData.id).url)
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultContainsEscapedMsgs(
          result,
          "agentCode.title",
          "agentCode.heading",
          "agentCode.hint",
          "agentCode.button"
        )
      }

      "show the prepopulated agent code page when record is found" in {
        val testData = MappingArnResult(
          arn = arn,
          agentCode = Some(saAgentCode)
        )
        await(repo.collection.insertOne(testData).toFuture())
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showAgentCode(testData.id).url)
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultContainsEscapedMsgs(
          result,
          "agentCode.title",
          "agentCode.heading",
          "agentCode.hint",
          "agentCode.button"
        )
        containSubstrings(
          saAgentCode
        )
      }

      "redirect to ASA home when no record is found" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showAgentCode("foo").url)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }
    }
    "the agent is on a client auth mapping journey" should {
      "show the agent code page when record is found" in {
        val testData = MappingArnResult(
          arn = arn,
          agentCode = None,
          legacyClientDetails = Some(LegacyClientDetails(
            "Client Name",
            Seq("A12345"),
            "/test-url",
            "/test-url"
          ))
        )
        await(repo.collection.insertOne(testData).toFuture())
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showAgentCode(testData.id).url)
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        checkHtmlResultContainsMsgsWithArg(
          result,
          Map(
            "agentCodeAuth.title" -> "",
            "agentCodeAuth.heading" -> "",
            "agentCodeAuth.para.1" -> "Client Name",
            "agentCodeAuth.para.2" -> "",
            "agentCodeAuth.para.3" -> "",
            "agentCodeAuth.bullet.1" -> "Client Name",
            "agentCodeAuth.bullet.2" -> "Client Name",
            "agentCodeAuth.insetText" -> "",
            "agentCodeAuth.details.summary" -> "",
            "agentCodeAuth.details.para.1" -> "",
            "agentCodeAuth.details.para.2" -> "",
            "agentCodeAuth.details.para.3" -> "",
            "agentCodeAuth.label" -> "",
            "agentCodeAuth.hint" -> "",
            "agentCodeAuth.button" -> "",
            "agentCodeAuth.cancelLink" -> "Client Name"
          )
        )
      }

      "redirect to ASA home when no record is found" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showAgentCode("foo").url)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }

      "redirect to ASA home when the journey is already complete" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showAgentCode("foo").url)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }
    }
  }

  "POST /agent-code" when {
    "the agent is on a mapping journey" should {
      "redirect to 'use GG id for agent code' when a valid agent code is submitted" in {
        val testData = MappingArnResult(
          arn = arn,
          agentCode = None
        )
        await(repo.collection.insertOne(testData).toFuture())
        givenUserIsAuthenticated(eligibleAgent)
        saMappingsFound(arn)

        val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode(testData.id).url)
          .withFormUrlEncodedBody("agentCode" -> saAgentCode)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(routes.MappingController.showUseTheGgUserId(testData.id).url)
        await(repo.findRecord(testData.id)) shouldBe Some(
          testData.copy(
            agentCode = Some(saAgentCode)
          )
        )
      }

      "return 400 when already mapped agent code is submitted" in {
        val testData = MappingArnResult(
          arn = arn,
          agentCode = None
        )
        await(repo.collection.insertOne(testData).toFuture())
        givenUserIsAuthenticated(eligibleAgent)
        saMappingsFound(arn)

        val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode(testData.id).url)
          .withFormUrlEncodedBody("agentCode" -> "A12345")
        val result = callEndpointWith(request)

        status(result) shouldBe 400
        checkHtmlResultContainsEscapedMsgs(
          result,
          "agentCode.error.alreadyMapped"
        )
      }

      val invalidCases = Seq(
        "" -> "agentCode.error.required",
        "A123456" -> "agentCode.error.length",
        "AB!@#4" -> "agentCode.error.format",
        "!" -> "agentCode.error.lengthAndFormat"
      )
      invalidCases.foreach { case (input, errorMsg) =>
        s"return 400 when invalid agent code $input is submitted" in {
          val testData = MappingArnResult(
            arn = arn,
            agentCode = None
          )
          await(repo.collection.insertOne(testData).toFuture())
          givenUserIsAuthenticated(eligibleAgent)
          saMappingsFound(arn)

          val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode(testData.id).url)
            .withFormUrlEncodedBody("agentCode" -> input)
          val result = callEndpointWith(request)

          status(result) shouldBe 400
          checkHtmlResultContainsEscapedMsgs(
            result,
            errorMsg
          )
        }
      }

      "redirect to ASA home when no record is found" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode("foo").url)
          .withFormUrlEncodedBody("agentCode" -> saAgentCode)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }
    }
    "the agent is on a client auth mapping journey" should {
      "redirect to 'use GG id for agent code' when a valid agent code is submitted" in {
        val testData = MappingArnResult(
          arn = arn,
          agentCode = None,
          legacyClientDetails = Some(LegacyClientDetails(
            "Client Name",
            Seq(saAgentCode),
            "/test-url",
            "/test-url"
          ))
        )
        await(repo.collection.insertOne(testData).toFuture())
        givenUserIsAuthenticated(eligibleAgent)

        val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode(testData.id).url)
          .withFormUrlEncodedBody("agentCode" -> saAgentCode)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(routes.MappingController.showUseTheGgUserId(testData.id).url)
        await(repo.findRecord(testData.id)) shouldBe Some(
          testData.copy(
            agentCode = Some(saAgentCode)
          )
        )
      }

      "return 400 when agent code that is not for this client is submitted" in {
        val testData = MappingArnResult(
          arn = arn,
          agentCode = None,
          legacyClientDetails = Some(LegacyClientDetails(
            "Client Name",
            Seq(saAgentCode),
            "/test-url",
            "/test-url"
          ))
        )
        await(repo.collection.insertOne(testData).toFuture())
        givenUserIsAuthenticated(eligibleAgent)
        saMappingsFound(arn)

        val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode(testData.id).url)
          .withFormUrlEncodedBody("agentCode" -> "A12345")
        val result = callEndpointWith(request)

        status(result) shouldBe 400
        checkHtmlResultContainsMsgsWithArg(
          result,
          Map("agentCodeAuth.error.wrongCode" -> "Client Name")
        )
      }

      "redirect to ASA home when no record is found" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode("foo").url)
          .withFormUrlEncodedBody("agentCode" -> saAgentCode)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }
      "redirect to ASA home when the journey is already complete" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest(POST, routes.MappingController.submitAgentCode("foo").url)
          .withFormUrlEncodedBody("agentCode" -> saAgentCode)
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }
    }
  }

  "GET /use-gg-user-id" should {
    "show the 'use GG user id' page for agent code page when record is found" in {
      val testData = MappingArnResult(
        arn = arn,
        agentCode = Some(saAgentCode)
      )
      await(repo.collection.insertOne(testData).toFuture())
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, routes.MappingController.showUseTheGgUserId(testData.id).url)
      val result = callEndpointWith(request)

      status(result) shouldBe 200
      checkHtmlResultContainsMsgsWithArg(
        result,
        Map(
          "userTheGgUserId.title" -> saAgentCode,
          "userTheGgUserId.heading" -> saAgentCode,
          "userTheGgUserId.para.1" -> "",
          "userTheGgUserId.para.2" -> saAgentCode,
          "userTheGgUserId.para.3" -> "",
          "userTheGgUserId.bullet.1" -> "",
          "userTheGgUserId.bullet.2" -> "",
          "userTheGgUserId.button" -> ""
        )
      )
    }

    "redirect to ASA home when no agent code is found" in {
      val testData = MappingArnResult(arn = arn)
      givenUserIsAuthenticated(eligibleAgent)

      val request = fakeRequest(GET, routes.MappingController.showUseTheGgUserId(testData.id).url)
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
    }

    "redirect to ASA home when no record is found" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, routes.MappingController.showUseTheGgUserId("foo").url)
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
    }
  }

  "GET /start-submit" should {
    val arn = Arn("TARN0000001")
    "redirect to client authorisations added for a user with IR-SA-AGENT enrolment after mapping and updating session" in {
      val testData = MappingArnResult(
        arn = arn,
        agentCode = Some(saAgentCode)
      )
      await(repo.collection.insertOne(testData).toFuture())
      givenClientCountRecordsFound(2)
      mappingIsCreated(arn)
      givenUserIsAuthenticated(eligibleAgent)

      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.MappingController.returnFromGGLogin(testData.id).url)
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.showClientAuthorisationsAdded(testData.id).url)
      await(repo.findRecord(testData.id)) shouldBe Some(
        testData.copy(
          agentCode = None,
          mappedAgentCode = Some(saAgentCode),
          mappedClientCount = Some(2)
        )
      )
    }

    "throw error if a user with IR-SA-AGENT enrolment has an identical code to an existing mapping" in {
      val testData = MappingArnResult(
        arn = arn,
        agentCode = Some(saAgentCode)
      )
      await(repo.collection.insertOne(testData).toFuture())
      mappingExists(arn)
      givenUserIsAuthenticated(eligibleAgent)

      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.MappingController.returnFromGGLogin(testData.id).url)
      val result = intercept[RuntimeException](callEndpointWith(request))

      result.getMessage should include("Agent is already mapped - unexpected state as this was checked earlier")
    }

    "throw error for a user with IR-SA-AGENT enrolment after mapping returns unexpected response" in {
      val testData = MappingArnResult(
        arn = arn,
        agentCode = Some(saAgentCode)
      )
      await(repo.collection.insertOne(testData).toFuture())
      mappingError(arn)
      givenUserIsAuthenticated(eligibleAgent)

      val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.MappingController.returnFromGGLogin(testData.id).url)
      val result = intercept[RuntimeException](callEndpointWith(request))

      result.getMessage should include("Unexpected response from mapping service: 403")
    }

    "redirect to error page if is a record with a different agentDode" in {
      val testData = MappingArnResult(
        arn = arn,
        agentCode = Some("AB1234")
      )
      await(repo.collection.insertOne(testData).toFuture())
      givenUserIsAuthenticated(eligibleAgent)
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.MappingController.returnFromGGLogin(testData.id).url)
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.problemWithDetails(testData.id).url)
    }

    "redirect to ASA home if is a record without an agentCode" in {
      val testData = MappingArnResult(arn = arn)
      await(repo.collection.insertOne(testData).toFuture())
      givenUserIsAuthenticated(eligibleAgent)
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.MappingController.returnFromGGLogin(testData.id).url)
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
    }

    "redirect to ASA home if there is no record found" in {
      givenUserIsAuthenticated(eligibleAgent)
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest(GET, routes.MappingController.returnFromGGLogin("foo").url)
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
    }
  }

  "GET /client-authorisations-added" should {
    val arn = Arn("TARN0000001")

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      GET,
      routes.MappingController.showClientAuthorisationsAdded(id = "someArnRefForMapping").url
    )(callEndpointWith)

    testsWithClientCount(0)
    testsWithClientCount(1)
    testsWithClientCount(10)
    authJourneyTestsWithClientCount(1)
    authJourneyTestsWithClientCount(10)

    // scalastyle:off method.length
    def testsWithClientCount(clientCount: Int): Unit =
      s"display the complete page with correct content for a user with" +
        s" enrolments: ${eligibleAgent.activeEnrolments.mkString(", ")} and a client count of $clientCount" in {
          val mappingId = repo.create(arn).futureValue
          val record = repo.findRecord(mappingId).futureValue.get
          repo.replace(
            record.copy(
              mappedClientCount = Some(clientCount),
              mappedAgentCode = Some(saAgentCode)
            ),
            mappingId
          ).futureValue

          givenUserIsAuthenticated(eligibleAgent)
          saMappingsFound(arn)
          val request = fakeRequest(GET, routes.MappingController.showClientAuthorisationsAdded(id = mappingId).url)
          val result = callEndpointWith(request)
          status(result) shouldBe 200
          val countSuffix =
            if clientCount == 0 then
              "none"
            else if clientCount == 1 then
              "single"
            else
              "multi"
          checkHtmlResultContainsMsgsWithArg(
            result,
            Map(
              s"authorisationsAdded.title.$countSuffix" -> clientCount.toString,
              s"authorisationsAdded.banner.header.$countSuffix" -> clientCount.toString,
              "authorisationsAdded.banner.body" -> saAgentCode,
              s"authorisationsAdded.para.1.$countSuffix" -> "",
              s"authorisationsAdded.para.2${if clientCount == 0 then
                  ".none"
                else
                  ""
                }" -> "",
              s"authorisationsAdded.inset.$countSuffix" -> saAgentCode,
              "authorisationsAdded.table.caption" -> "",
              "authorisationsAdded.table.agentReference" -> "",
              "authorisationsAdded.table.dateCreated" -> "",
              "authorisationsAdded.link.addAnother" -> "",
              "authorisationsAdded.link.asa" -> ""
            )
          )
        }
    def authJourneyTestsWithClientCount(clientCount: Int): Unit =
      s"display the complete page with correct content for a user on a client auth journey with" +
        s" enrolments: ${eligibleAgent.activeEnrolments.mkString(", ")} and a client count of $clientCount" in {
          val mappingId = repo.create(arn).futureValue
          val record = repo.findRecord(mappingId).futureValue.get
          repo.replace(
            record.copy(
              legacyClientDetails = Some(
                LegacyClientDetails(
                  "Client Name",
                  Seq(saAgentCode),
                  "/continue-url",
                  "/back-url"
                )
              ),
              mappedClientCount = Some(clientCount),
              mappedAgentCode = Some(saAgentCode)
            ),
            mappingId
          ).futureValue

          givenUserIsAuthenticated(eligibleAgent)
          saMappingsFound(arn)
          val request = fakeRequest(GET, routes.MappingController.showClientAuthorisationsAdded(id = mappingId).url)
          val result = callEndpointWith(request)
          status(result) shouldBe 200
          val countSuffix =
            if clientCount == 1 then
              "single"
            else
              "multi"
          checkHtmlResultContainsMsgsWithArgs(
            result,
            Map(
              s"authorisationsAdded.title.$countSuffix" -> Seq(clientCount.toString),
              s"authorisationsAdded.banner.header.$countSuffix" -> Seq(clientCount.toString),
              "authorisationsAdded.banner.body" -> Seq(saAgentCode),
              s"authorisationsAdded.para.1.$countSuffix.named" -> Seq(saAgentCode, "Client Name"),
              "authorisationsAdded.para.2" -> Nil,
              s"authorisationsAdded.inset.$countSuffix" -> Seq(saAgentCode),
              "authorisationsAdded.link.asa" -> Nil
            )
          )
        }

    s"redirect to journey start when repository does not hold the record for the user with" +
      s" enrolment ${eligibleAgent.activeEnrolments.mkString(", ")}" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showClientAuthorisationsAdded(id = "someArnRefForMapping").url)
        val result = callEndpointWith(request)
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }

    s"redirect to journey start when repository does not hold the mapped agent code and count for the user with" +
      s" enrolment ${eligibleAgent.activeEnrolments.mkString(", ")}" in {
        val mappingId = await(repo.create(arn))
        val record = await(repo.findRecord(mappingId)).get
        await(
          repo.replace(
            record.copy(
              agentCode = Some(saAgentCode)
            ),
            mappingId
          )
        )

        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(GET, routes.MappingController.showClientAuthorisationsAdded(id = mappingId).url)
        val result = callEndpointWith(request)
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("http://localhost:9401/agent-services-account")
      }
  }

  "/wrong-sign-in-asa" should {
    "contain a message indicating that the user is logged in with an ASA" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(GET, routes.MappingController.wrongSignInDetailsAsa(id = "someArnRefForMapping").url)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "wrongSignInDetails.title",
        "wrongSignInDetails.heading",
        "wrongSignInDetails.para.1.asa",
        "wrongSignInDetails.para.2",
        "wrongSignInDetails.button"
      )
    }
  }

  "/wrong-sign-in-not-agent" should {
    "contain a message indicating that the user not logged in with an agent account" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(GET, routes.MappingController.wrongSignInDetailsNotAgent(id = "someArnRefForMapping").url)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "wrongSignInDetails.title",
        "wrongSignInDetails.heading",
        "wrongSignInDetails.para.1.notAgent",
        "wrongSignInDetails.para.2",
        "wrongSignInDetails.button"
      )
    }
  }

  "/problem-with-details" should {
    "contain a message indicating that there is a problem matching the users cred to the agent code" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(GET, routes.MappingController.problemWithDetails(id = "someArnRefForMapping").url)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "problemWithDetails.title",
        "problemWithDetails.heading",
        "problemWithDetails.para.1",
        "problemWithDetails.para.2",
        "problemWithDetails.bullet.1",
        "problemWithDetails.bullet.2",
        "problemWithDetails.bullet.3",
        "problemWithDetails.button"
      )
    }
  }
