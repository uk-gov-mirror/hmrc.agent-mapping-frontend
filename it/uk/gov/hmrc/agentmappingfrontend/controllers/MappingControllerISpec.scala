package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.auth.Auth
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.{mappingExists, mappingIsCreated, mappingKnownFactsIssue}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

class MappingControllerISpec extends BaseControllerISpec with AuthStubs {

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
    "display the start page" in {
      val request = FakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("connectAgentServices.start.title"))
    }
  }

  "startSubmit" should {

    behave like anAuthenticatedEndpoint(GET, "/agent-mapping/start-submit", callEndpointWith)

    Auth.validEnrolments.foreach { serviceName =>
      s"redirect to the add-code if the current user is logged in and has legacy agent enrolment for $serviceName" in {
        givenAuthorisedFor(serviceName)
        val request = fakeRequest(GET, "/agent-mapping/start-submit")
        val result = callEndpointWith(request)
        status(result) shouldBe 303
        redirectLocation(result).get shouldBe routes.MappingController.showAddCode().url
      }
    }
  }

  "show add-code" should {

    val endpoint = "/agent-mapping/add-code"

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      GET,
      endpoint,
      expectCheckAgentRefCodeAudit = true)(callEndpointWith)

    "display the add code page if the current user is logged in and has legacy agent enrolment for SA" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include("Connect to your agent services account")
      verifyCheckAgentRefCodeAuditEvent(activeEnrolments = eligibleAgent.activeEnrolments)
    }

    "display the add code page if the current user is logged in and has legacy agent enrolment for VAT" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include("Connect to your agent services account")
      verifyCheckAgentRefCodeAuditEvent(activeEnrolments = vatEnrolledAgent.activeEnrolments)
    }

    "display the SA Agent Reference if the current user is logged in and has legacy agent enrolment for SA" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      verifyCheckAgentRefCodeAuditEvent(activeEnrolments = eligibleAgent.activeEnrolments)
    }

    "display the VAT Agent Reference if the current user is logged in and has legacy agent enrolment for VAT" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      verifyCheckAgentRefCodeAuditEvent(activeEnrolments = vatEnrolledAgent.activeEnrolments)
    }
  }

  "submit add code" should {

    val endpoint = "/agent-mapping/add-code"

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      POST,
      endpoint,
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    "redirect to complete if the user enters an ARN and UTR that match the known facts for SA" in {
      givenUserIsAuthenticated(eligibleAgent)
      mappingIsCreated(Utr("2000000000"), Arn("TARN0000001"))
      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete().url
    }

    "redirect to complete if the user enters an ARN and UTR that match the known facts for VAT" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      mappingIsCreated(Utr("2000000000"), Arn("TARN0000001"))
      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete().url
    }

    "redirect to the already-mapped page if the mapping already exists for SA" in new App {
      givenUserIsAuthenticated(eligibleAgent)
      mappingExists(Utr("2000000000"), Arn("TARN0000001"))

      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.alreadyMapped().url
    }

    "redirect to the already-mapped page if the mapping already exists for VAT" in new App {
      givenUserIsAuthenticated(vatEnrolledAgent)
      mappingExists(Utr("2000000000"), Arn("TARN0000001"))

      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.alreadyMapped().url
    }

    "redisplay the form " when {
      "there is no ARN " in {
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("This field is required")
        bodyOf(result) should include("2000000000")
      }

      "the arn is invalid" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "ARN0000001", "utr.value" -> "2000000000")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("Check you have entered a valid account number")
        bodyOf(result) should include("2000000000")
        bodyOf(result) should include("ARN0000001")
      }

      "there is no UTR " in {
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("This field is required")
        bodyOf(result) should include("TARN0000001")
      }

      "the utr is invalid" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "notautr")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("Check you have entered a valid UTR or tax reference")
        bodyOf(result) should include("notautr")
        bodyOf(result) should include("TARN0000001")
      }

      "the known facts check fails" in {
        givenUserIsAuthenticated(eligibleAgent)
        mappingKnownFactsIssue(Utr("2000000000"), Arn("TARN0000001"))

        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("arn.arn" -> "TARN0000001", "utr.value" -> "2000000000")
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include(htmlEscapedMessage("error.summary.heading"))
      }
    }
  }

  "complete" should {

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      GET,
      s"/agent-mapping/complete",
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    "display the complete page for an arn and ir sa agent reference" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, s"/agent-mapping/complete")
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("connectionComplete.title"))
      resultBody should include(htmlEscapedMessage("button.repeatProcess"))
      resultBody should include(htmlEscapedMessage("button.signOut"))
    }

    "display the complete page for an arn and vat agent reference" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(GET, s"/agent-mapping/complete")
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("connectionComplete.title"))
      resultBody should include(htmlEscapedMessage("button.repeatProcess"))
      resultBody should include(htmlEscapedMessage("button.signOut"))
    }
  }

  "not enrolled " should {
    "contain a message indicating that the user is not enrolled for a valid non-mtd enrolment" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(GET, "/agent-mapping/not-enrolled")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("notEnrolled.p1"))
    }
  }

  "already mapped " should {
    "contain a message indicating that the user has already mapped all of her non-mtd identifiers" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, "/agent-mapping/errors/already-linked")
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("alreadyMapped.title"))
      resultBody should include(htmlEscapedMessage("alreadyMapped.p1"))
      resultBody should include(htmlEscapedMessage("alreadyMapped.p2"))
      resultBody should include(htmlEscapedMessage("button.startNow"))
    }
  }
}
