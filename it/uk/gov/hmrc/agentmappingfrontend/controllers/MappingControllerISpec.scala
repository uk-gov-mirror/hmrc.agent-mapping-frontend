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
      s"redirect to the enter-account-number if the current user is logged in and has legacy agent enrolment for $serviceName" in {
        givenAuthorisedFor(serviceName)
        val request = fakeRequest(GET, "/agent-mapping/start-submit")
        val result = callEndpointWith(request)
        status(result) shouldBe 303
        redirectLocation(result).get shouldBe routes.MappingController.showEnterAccountNo().url
      }
    }
  }

  "show enter-account-number" should {

    val endpoint = "/agent-mapping/enter-account-number"

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      GET,
      endpoint,
      expectCheckAgentRefCodeAudit = true)(callEndpointWith)

    "display the enter utr page if the current user is logged in and has legacy agent enrolment for SA" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("enter-account-number.title"))
      verifyCheckAgentRefCodeAuditEvent(activeEnrolments = eligibleAgent.activeEnrolments)
    }

    "display the enter utr page if the current user is logged in and has legacy agent enrolment for VAT" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(GET, endpoint)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("enter-account-number.title"))
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

  "submit enter-account-number" should {
    val createRequest = (arn: String) => fakeRequest(POST, routes.MappingController.submitEnterAccountNo.toString)
      .withFormUrlEncodedBody("arn.arn" -> s"$arn")

    "redirect and add arn to session" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = createRequest("TARN0000001")
      val result = callEndpointWith(request)
      redirectLocation(result) shouldBe Some(routes.MappingController.showEnterUtr.url)
      result.session(request).get("mappingArn") shouldBe Some("TARN0000001")
    }

    "re-enter arn since invalid" in {
      givenUserIsAuthenticated(eligibleAgent)
      val result = callEndpointWith(createRequest("invalidArn"))
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("error.arn.invalid"))
    }

    "re-enter arn since empty" in {
      givenUserIsAuthenticated(eligibleAgent)
      val result = callEndpointWith(createRequest(""))
      status(result) shouldBe 200
      bodyOf(result) should include("This field is required")
    }
  }

  "show enter-utr" should {
    def request = fakeRequest(GET, routes.MappingController.showEnterUtr.url)

    "enter-utr arn found in session" in {
      givenUserIsAuthenticated(eligibleAgent)

      val result = callEndpointWith(request.withSession(("mappingArn", "TARN0000001")))
      status(result) shouldBe 200
    }

    "redirect noArn go back to enter-account-number" in {
      givenUserIsAuthenticated(eligibleAgent)

      val result = callEndpointWith(request)
      redirectLocation(result) shouldBe Some(routes.MappingController.showEnterAccountNo.url)
    }
  }

  "submit add code" should {

    val endpoint = "/agent-mapping/enter-utr"

    behave like anEndpointReachableIfSignedInWithEligibleEnrolment(
      POST,
      endpoint,
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    "redirect to complete if the user enters an ARN and UTR that match the known facts for SA" in {
      givenUserIsAuthenticated(eligibleAgent)
      mappingIsCreated(Utr("2000000000"), Arn("TARN0000001"))
      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000").withSession(("mappingArn", "TARN0000001"))
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete().url
    }

    "redirect to complete if the user enters an ARN and UTR that match the known facts for VAT" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      mappingIsCreated(Utr("2000000000"), Arn("TARN0000001"))
      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000").withSession(("mappingArn", "TARN0000001"))
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.complete().url
    }

    "redirect to the already-mapped page if the mapping already exists for SA" in new App {
      givenUserIsAuthenticated(eligibleAgent)
      mappingExists(Utr("2000000000"), Arn("TARN0000001"))

      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000").withSession(("mappingArn", "TARN0000001"))
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.alreadyMapped().url
    }

    "redirect to the already-mapped page if the mapping already exists for VAT" in new App {
      givenUserIsAuthenticated(vatEnrolledAgent)
      mappingExists(Utr("2000000000"), Arn("TARN0000001"))

      val request =
        fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000").withSession(("mappingArn", "TARN0000001"))
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.MappingController.alreadyMapped().url
    }

    "redisplay the form " when {
      "there is no UTR " in {
        givenUserIsAuthenticated(eligibleAgent)
        val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "").withSession(("mappingArn", "TARN0000001"))
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("This field is required")
      }

      "the utr is invalid" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "invalidUtr").withSession(("mappingArn", "TARN0000001"))
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include("Check you have entered a valid UTR or tax reference")
      }

      "the known facts check fails" in {
        givenUserIsAuthenticated(eligibleAgent)
        mappingKnownFactsIssue(Utr("2000000000"), Arn("TARN0000001"))

        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000").withSession(("mappingArn", "TARN0000001"))
        val result = callEndpointWith(request)

        redirectLocation(result) shouldBe Some(routes.MappingController.noMatch.url)
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