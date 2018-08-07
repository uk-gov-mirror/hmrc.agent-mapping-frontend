package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.auth.Auth
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnRepository
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.{mappingExists, mappingIsCreated}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.{eligibleAgent, _}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext.Implicits.global

class MappingControllerISpec extends BaseControllerISpec with AuthStubs {

  private lazy val repo = app.injector.instanceOf[MappingArnRepository]


  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "context root" should {
    "redirect to the start page" in {
      val request = FakeRequest(GET, "/agent-mapping/")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).head should include("/start")
    }
  }

  "start" should {
    "200 the start page if user has HMRC-AS-AGENT and 'Sign in with another account' button holds idReference to agent's ARN" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = FakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsMsgs(result, "connectAgentServices.start.title", "button.startNow")
      bodyOf(result) should include("/signed-out-redirect?id=")
    }

    "303 the /sign-in-required for unAuthenticated" in {
      givenUserIsNotAuthenticated
      val request = FakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      redirectLocation(result) shouldBe Some(routes.MappingController.needAgentServicesAccount().url)
    }

    "303 to /sign-in-required when user without HMRC-AS-AGENT/ARN" in {
      givenAuthorisedFor("notHMRCASAGENT")
      val request = FakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.needAgentServicesAccount().url)
    }
  }

  "/sign-in-required" should {
    "200 the /start/sign-in-required page when not logged in" in {
      givenUserIsNotAuthenticated
      val request = FakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsMsgs(result, "start.not-signed-in.title")
    }

    "200 the /sign-in-required page as NO ARN is found" in {
      givenAuthorisedFor("notHMRCASAGENT")
      val request = FakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsMsgs(result, "start.not-signed-in.title", "button.signIn")
    }

    "303 the /start page when user has HMRC-AS-AGENT/ARN and 'Sign in with another account' button holds idReference to agent's ARN" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = FakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      redirectLocation(result) shouldBe Some(routes.MappingController.start().url)
    }
  }

  "startSubmit" should {
    val arn = Arn("TARN0000001")
    Auth.validEnrolments.foreach { serviceName =>
      s"303 to /account-linked when successfully mapped legacy enrolment identifier for $serviceName, then test arnRef no longer valid" in {
        val persistedMappingArnResultId = await(repo.create(arn))
        mappingIsCreated(arn)
        givenAuthorisedFor(serviceName)
        implicit val request = fakeRequest(GET, s"/agent-mapping/start-submit?id=$persistedMappingArnResultId")
        val result = callEndpointWith(request)

        status(result) shouldBe 303
        redirectLocation(result).get should include(routes.MappingController.complete(id = "").url)

        givenAuthorisedFor(serviceName)
        val requestWithUsedIdShouldFail = fakeRequest(GET, s"/agent-mapping/start-submit?id=$persistedMappingArnResultId")
        val resultCopiedAttempt = callEndpointWith(requestWithUsedIdShouldFail)
        redirectLocation(resultCopiedAttempt) shouldBe Some(routes.MappingController.start().url)
      }
    }

    s"303 to /account-linked persistedMappingArnResultId is INVALID" in {
      givenAuthorisedFor("IR-SA-AGENT")
      val request = fakeRequest(GET, s"/agent-mapping/start-submit?id=meaninglessBlaBlaID")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.start().url
    }

    s"303 to /already-mapped when all available identifiers have been mapped" in {
      mappingExists(arn)
      givenAuthorisedFor("IR-SA-AGENT")
      val attempt2Id = await(repo.create(arn))
      val attempt2Request = fakeRequest(GET, s"/agent-mapping/start-submit?id=$attempt2Id")
      val attempt2Result = callEndpointWith(attempt2Request)

      status(attempt2Result) shouldBe 303
      redirectLocation(attempt2Result).get should include(routes.MappingController.alreadyMapped(id = "").url)
    }
  }

  "complete" should {
    val arn = Arn("TARN0000001")

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      GET,
      routes.MappingController.complete(id = "someArnRefForMapping").url,
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    for(user <- Seq(eligibleAgent, vatEnrolledAgent)) {
      s"display the complete page with correct content for a user with enrolments: ${user.activeEnrolments.mkString(", ")}" in {
        val persistedMappingArnResultId = await(repo.create(arn))
        givenUserIsAuthenticated(user)
        val request = fakeRequest(GET, routes.MappingController.complete(id = persistedMappingArnResultId).url)
        val result = callEndpointWith(request)
        status(result) shouldBe 200
        checkHtmlResultContainsMsgs(result, "connectionComplete.title",
          "button.repeatProcess",
          "link.finishSignOut",
          "connectionComplete.banner.header",
          "connectionComplete.banner.paragraph")
      }

      s"return an exception when repository does not hold the record for the user with enrolment ${user.activeEnrolments.mkString(", ")}" in {
        givenUserIsAuthenticated(user)
        val request = fakeRequest(GET, routes.MappingController.complete(id = "someArnRefForMapping").url)
        an[InternalServerException] shouldBe thrownBy(callEndpointWith(request))
      }
    }
  }

  "not enrolled " should {
    "contain a message indicating that the user is not enrolled for a valid non-mtd enrolment" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(GET, routes.MappingController.notEnrolled(id = "someArnRefForMapping").url)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsMsgs(result,"notEnrolled.p1", "button.tryAgain")
    }
  }

  "already mapped " should {
    "contain a message indicating that the user has already mapped all of her non-mtd identifiers" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, routes.MappingController.alreadyMapped(id = "someArnRefForMapping").url)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsMsgs(result, "error.title",
        "alreadyMapped.p1",
        "alreadyMapped.p2",
        "button.tryAgain")
    }
  }

  "incorrectAccount" should {
    trait IncorrectAccountFixture {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = fakeRequest(GET, routes.MappingController.incorrectAccount(id = "someArnRefForMapping").url)
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
    }

    "contain a Try Again button for signing in again and repeating the journey" in new IncorrectAccountFixture {
      checkHtmlResultContainsMsgs(result, "button.tryAgain")
      resultBody should include(""" href="/agent-mapping/signed-out-redirect?id=""")
    }

    "contain a link to Agent Services Account homepage" in new IncorrectAccountFixture {
      checkHtmlResultContainsMsgs(result,"link.goToASAccount")
      resultBody should include(""" href="http://localhost:9401/agent-services-account" """)
    }

    "return 200 response and contain appropriate content" in new IncorrectAccountFixture {
      status(result) shouldBe 200
      checkHtmlResultContainsMsgs(result,"error.title",
        "incorrectAccount.p1",
        "incorrectAccount.p2"
      )
    }
  }
}