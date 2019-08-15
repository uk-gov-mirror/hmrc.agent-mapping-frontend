package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.auth.Auth
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnRepository
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
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
      checkHtmlResultContainsEscapedMsgs(
        result,
        "connectAgentServices.start.whatYouNeedToKnow.heading",
        "connectAgentServices.start.whatYouNeedToKnow.p1",
        "connectAgentServices.start.inset",
        "connectAgentServices.start.whatYouNeedToDo.heading",
        "connectAgentServices.start.whatYouNeedToDo.p1",
        "connectAgentServices.start.whatYouNeedToDo.p2",
        "button.continue"
      )
      checkHtmlResultContainsMsgs(result, "connectAgentServices.start.whatYouNeedToKnow.p1")
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
      checkHtmlResultContainsEscapedMsgs(result, "start.not-signed-in.title")
    }

    "200 the /sign-in-required page as NO ARN is found" in {
      givenAuthorisedFor("notHMRCASAGENT")
      val request = FakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "start.not-signed-in.title", "button.signIn")
    }

    "303 the /start page when user has HMRC-AS-AGENT/ARN and 'Sign in with another account' button holds idReference to agent's ARN" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = FakeRequest(GET, "/agent-mapping/sign-in-required")
      val result = callEndpointWith(request)
      redirectLocation(result) shouldBe Some(routes.MappingController.start().url)
    }
  }

  import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs._

  "/start-submit" should {

    redirectFromGGLoginTests(true)
    redirectFromGGLoginTests(false)

    def redirectFromGGLoginTests(singleClientCountResponse: Boolean) = {
      val arn = Arn("TARN0000001")
      Auth.validEnrolments.foreach { serviceName =>
        s"303 to /client-relationships-found for $serviceName and for a single client relationship $singleClientCountResponse" in {
          val id = await(repo.create(arn))
          if (singleClientCountResponse) givenClientCountRecordsFound(1)
          else givenClientCountRecordsFound(12)
          givenAuthorisedFor(serviceName)
          implicit val request = fakeRequest(GET, s"/agent-mapping/start-submit?id=$id")
          val result = callEndpointWith(request)

          status(result) shouldBe 303
        }
      }
    }

    "redirect to start if there is no record found" in {
      givenAuthorisedFor("IR-SA-AGENT")
      implicit val request = fakeRequest(GET, s"/agent-mapping/start-submit?id=foo")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.start().url)
    }
  }

  "/client-relationships-found" should {

    testsForClientRelationshipsFound(true)
    testsForClientRelationshipsFound(false)

    def testsForClientRelationshipsFound(singleClientCountResponse: Boolean): Unit = {
      val arn = Arn("TARN0000001")
      Auth.validEnrolments.foreach { serviceName =>
        s"200 to /client-relationships-found for $serviceName and for a single client relationship $singleClientCountResponse" in {
          val clientCount = if (singleClientCountResponse) 1 else 12
          val id = await(repo.create(arn, List(clientCount)))
          givenAuthorisedFor(serviceName)
          implicit val request = fakeRequest(GET, s"/agent-mapping/client-relationships-found?id=$id")
          val result = callEndpointWith(request)

          if (singleClientCountResponse) {
            checkHtmlResultContainsEscapedMsgs(
              result,
              "clientRelationshipsFound.single.title",
              "clientRelationshipsFound.single.p1",
              "clientRelationshipsFound.single.td",
              "clientRelationshipsFound.single.p2"
            )
          } else {
            checkHtmlResultContainsEscapedMsgs(
              result,
              "clientRelationshipsFound.multi.title",
              "clientRelationshipsFound.multi.p1",
              "clientRelationshipsFound.multi.td",
              "clientRelationshipsFound.multi.p2"
            )
          }
        }
      }
    }

    "display page not found if there is no record found" in {
      givenAuthorisedFor("IR-SA-AGENT")
      implicit val request = fakeRequest(GET, s"/agent-mapping/client-relationships-found?id=foo")
      val result = callEndpointWith(request)

      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "page-not-found.title", "page-not-found.h1", "page-not-found.p1")
    }
  }

  "/tag-gg GET" should {
    val arn = Arn("TARN0000001")
    "display the GGTag screen" in {
      val clientCount = 12
      val id = await(repo.create(arn, List(clientCount)))
      givenAuthorisedFor("IR-SA-AGENT")
      implicit val request = fakeRequest(GET, s"/agent-mapping/tag-gg?id=$id")
      val result = callEndpointWith(request)

      checkHtmlResultContainsEscapedMsgs(
        result,
        "gg-tag.title",
        "gg-tag.p1",
        "gg-tag.form.identifier",
        "gg-tag.form.hint",
        "gg-tag.xs")
    }
  }

  "/tag-gg POST" should {
    val arn = Arn("TARN0000001")
    "redirect to existing-client-relationships when a valid gg-tag is submitted" in {
      val clientCount = 12
      val id = await(repo.create(arn, List(clientCount)))
      givenAuthorisedFor("IR-SA-AGENT")
      implicit val request = fakeRequest(POST, s"/agent-mapping/tag-gg?id=$id")
        .withFormUrlEncodedBody("ggTag" -> "1234")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.showExistingClientRelationships(id).url)
    }

    "redisplay the page with errors when an invalid gg-tag is submitted" in {
      val clientCount = 12
      val id = await(repo.create(arn, List(clientCount)))
      givenAuthorisedFor("IR-SA-AGENT")
      implicit val request = fakeRequest(POST, s"/agent-mapping/tag-gg?id=$id")
        .withFormUrlEncodedBody("ggTag" -> "abcd")
      val result = callEndpointWith(request)

      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "gg-tag.title", "error.gg-tag.invalid")
    }
  }

  "/existing-client-relationships - GET" should {

    testsForExistingClientRelationships(true)
    testsForExistingClientRelationships(false)

    def testsForExistingClientRelationships(singleClientCountResponse: Boolean): Unit = {
      val arn = Arn("TARN0000001")
      Auth.validEnrolments.foreach { serviceName =>
        s"200 to /existing-client-relationships for $serviceName and for a single client relationship $singleClientCountResponse" in {

          val clientCount = if (singleClientCountResponse) 1 else 12
          val id = await(repo.create(arn, List(clientCount)))
          givenAuthorisedFor(serviceName)
          mappingIsCreated(arn)
          implicit val request = fakeRequest(GET, s"/agent-mapping/existing-client-relationships?id=$id")
          val result = callEndpointWith(request)

          checkHtmlResultContainsEscapedMsgs(
            result,
            "existingClientRelationships.title",
            "existingClientRelationships.td",
            "existingClientRelationships.heading",
            "existingClientRelationships.p1",
            "existingClientRelationships.yes",
            "existingClientRelationships.no"
          )
          if (singleClientCountResponse) {
            bodyOf(result) should include(htmlEscapedMessage("existingClientRelationships.single.th"))
          } else {
            bodyOf(result) should include(htmlEscapedMessage("existingClientRelationships.multi.th"))
          }

        }
      }
    }

    "display the existing client relationships page if the user is already mapped" in {
      val arn = Arn("TARN0000001")
      val clientCount = 12
      val id = await(repo.create(arn, List(clientCount)))
      await(repo.updateFor(id))
      givenAuthorisedFor("IR-SA-AGENT")
      implicit val request = fakeRequest(GET, s"/agent-mapping/existing-client-relationships?id=$id")
      val result = callEndpointWith(request)

      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "existingClientRelationships.title",
        "existingClientRelationships.td",
        "existingClientRelationships.heading",
        "existingClientRelationships.p1",
        "existingClientRelationships.yes",
        "existingClientRelationships.no"
      )
    }

    "redirect to already mapped when mapping creation returns a conflict" in {
      val arn = Arn("TARN0000001")
      val clientCount = 12
      val id = await(repo.create(arn, List(clientCount)))
      givenAuthorisedFor("IR-SA-AGENT")
      mappingExists(arn)
      implicit val request = fakeRequest(GET, s"/agent-mapping/existing-client-relationships?id=$id")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.alreadyMapped(id).url)
    }

    "throw an internal server error when the response from mapping creation is unknown" in {
      val arn = Arn("TARN0000001")
      val clientCount = 12
      val id = await(repo.create(arn, List(clientCount)))
      givenAuthorisedFor("IR-SA-AGENT")
      mappingKnownFactsIssue(arn)
      implicit val request = fakeRequest(GET, s"/agent-mapping/existing-client-relationships?id=$id")
      val result = callEndpointWith(request)

      status(result) shouldBe 500
    }

    "show the page not found page if there is not record" in {
      givenAuthorisedFor("IR-SA-AGENT")
      implicit val request = fakeRequest(GET, s"/agent-mapping/existing-client-relationships?id=foo")
      val result = callEndpointWith(request)

      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "page-not-found.title",
        "page-not-found.h1",
        "page-not-found.p1"
      )
    }
  }

  "/existing-client-relationships - POST" should {
    val arn = Arn("TARN0000001")

    "redirect to /complete when the user selects NO" in {
      val persistedMappingArnResultId = await(repo.create(arn))
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(
        POST,
        routes.MappingController.submitExistingClientRelationships(id = persistedMappingArnResultId).url)
        .withFormUrlEncodedBody("additional-clients" -> "no")

      val result = callEndpointWith(request)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.MappingController.complete(persistedMappingArnResultId).url)
    }

    "redirect to gg/sign-in when the user selects YES" in {
      val persistedMappingArnResultId = await(repo.create(arn))
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(
        POST,
        routes.MappingController.submitExistingClientRelationships(id = persistedMappingArnResultId).url)
        .withFormUrlEncodedBody("additional-clients" -> "yes")

      val result = callEndpointWith(request)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(
        routes.SignedOutController.signOutAndRedirect(persistedMappingArnResultId).url)
    }

    "200 OK to /existing-clients with error message when inputs invalid data" in {
      val persistedMappingArnResultId = await(repo.create(arn, List(1)))
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(
        POST,
        routes.MappingController.submitExistingClientRelationships(id = persistedMappingArnResultId).url)
        .withFormUrlEncodedBody("additional-clients" -> "blah")

      val result = callEndpointWith(request)

      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(
        result,
        "error.existingClientRelationships.choice.invalid",
        "existingClientRelationships.title",
        "existingClientRelationships.td",
        "existingClientRelationships.heading",
        "existingClientRelationships.p1",
        "existingClientRelationships.yes",
        "existingClientRelationships.no"
      )

      bodyOf(result) should include(htmlEscapedMessage("existingClientRelationships.single.th"))
    }

    "redirect to start when there is a form error and no record found" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(
        POST,
        routes.MappingController.submitExistingClientRelationships(id = "foo").url)
        .withFormUrlEncodedBody("additional-clients" -> "foo")

      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.MappingController.start().url)
    }

  }

  "complete" should {
    val arn = Arn("TARN0000001")

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      GET,
      routes.MappingController.complete(id = "someArnRefForMapping").url,
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    testsForComplete(true)
    testsForComplete(false)

    def testsForComplete(singleClientCountResponse: Boolean) =
      for (user <- Seq(eligibleAgent, vatEnrolledAgent)) {
        s"display the complete page with correct content for a user with enrolments: ${user.activeEnrolments.mkString(
          ", ")} and single client response: $singleClientCountResponse" in {

          val clientCount = if (singleClientCountResponse) 1 else 12
          val persistedMappingArnResultId = await(repo.create(arn, List(clientCount)))
          givenUserIsAuthenticated(user)
          val request = fakeRequest(GET, routes.MappingController.complete(id = persistedMappingArnResultId).url)
          val result = callEndpointWith(request)
          status(result) shouldBe 200
          checkHtmlResultContainsEscapedMsgs(
            result,
            "connectionComplete.title",
            "connectionComplete.banner.header",
            "connectionComplete.h3.1",
            "connectionComplete.finish")

          result should containLink("connectionComplete.finish", routes.SignedOutController.reLogForMappingStart().url)
          result should containLink(
            "connectionComplete.mtdLink",
            "https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")

          if (singleClientCountResponse)
            result should containSubstrings("You copied 1 client relationship to your agent services account")
          else result should containSubstrings("You copied 12 client relationships to your agent services account")

          result should containSubstrings(
            "To submit VAT returns digitally for a client, you now need to",
            "sign your client up for Making Tax Digital for VAT (opens in a new window or tab).")
        }

        s"return an exception when repository does not hold the record for the user with enrolment ${user.activeEnrolments
          .mkString(", ")} and single client response $singleClientCountResponse" in {
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
      checkHtmlResultContainsEscapedMsgs(result, "notEnrolled.p1", "button.tryAgain")
    }
  }

  "already mapped " should {
    "contain a message indicating that the user has already mapped all of her non-mtd identifiers" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, routes.MappingController.alreadyMapped(id = "someArnRefForMapping").url)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(
        result,
        "error.title",
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
      checkHtmlResultContainsEscapedMsgs(result, "button.tryAgain")
      resultBody should include(""" href="/agent-mapping/signed-out-redirect?id=""")
    }

    "contain a link to Agent Services Account homepage" in new IncorrectAccountFixture {
      checkHtmlResultContainsEscapedMsgs(result, "link.goToASAccount")
      resultBody should include(""" href="http://localhost:9401/agent-services-account" """)
    }

    "return 200 response and contain appropriate content" in new IncorrectAccountFixture {
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "error.title", "incorrectAccount.p1", "incorrectAccount.p2")
    }
  }
}
