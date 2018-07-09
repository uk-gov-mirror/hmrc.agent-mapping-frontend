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
import uk.gov.hmrc.http.InternalServerException

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
    "display the start page with ARN if user has HMRC-AS-AGENT" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = FakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("connectAgentServices.start.title"))
      bodyOf(result) should include("TARN-000-0001")
    }

    "display the start page for unAuthenticated user" in {
      givenUserIsNotAuthenticated
      val request = FakeRequest(GET, "/agent-mapping/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("connectAgentServices.start.title"))
    }
  }

  "startSubmit" should {

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

    "redirect and add arn to session when arn in hyphen pattern is entered" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = createRequest("TARN-000-0001")
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

    "re-enter arn if an arn entered with invalid format" in {
      givenUserIsAuthenticated(eligibleAgent)
      val result = callEndpointWith(createRequest("TARN-0000-001"))
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("error.arn.invalid"))
    }

    "re-enter arn since empty" in {
      givenUserIsAuthenticated(eligibleAgent)
      val result = callEndpointWith(createRequest(""))
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("error.arn.blank"))
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
      val request = fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000000000")
        .withSession(("mappingArn", "TARN0000001"))
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
        bodyOf(result) should include(htmlEscapedMessage("error.utr.blank"))
      }

      "the utr is invalid" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "invalidUtr").withSession(("mappingArn", "TARN0000001"))
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include(htmlEscapedMessage("error.utr.invalid.format"))
      }


      "the utr has wrong long length" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "200000000099").withSession(("mappingArn", "TARN0000001"))
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include(htmlEscapedMessage("error.utr.invalid.length"))
      }

      "the utr has wrong short length" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "2000").withSession(("mappingArn", "TARN0000001"))
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include(htmlEscapedMessage("error.utr.invalid.length"))
      }

      "the utr with spaces has wrong length" in {
        givenUserIsAuthenticated(eligibleAgent)
        val request =
          fakeRequest(POST, endpoint).withFormUrlEncodedBody("utr.value" -> "200000 000").withSession(("mappingArn", "TARN0000001"))
        val result = callEndpointWith(request)

        status(result) shouldBe 200
        bodyOf(result) should include(htmlEscapedMessage("error.utr.invalid.length"))
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
      routes.MappingController.complete.url,
      expectCheckAgentRefCodeAudit = false)(callEndpointWith)

    "display the complete page for an arn and ir sa agent reference" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, routes.MappingController.complete.url).withSession(("mappingArn", "TARN0000001"))
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("connectionComplete.title"))
      resultBody should include(htmlEscapedMessage("button.repeatProcess"))
      resultBody should include(htmlEscapedMessage("link.finishSignOut"))
      resultBody should include("TARN-000-0001")
      resultBody should include("12345-credId")
    }

    "display the complete page for an arn and vat agent reference" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = fakeRequest(GET, routes.MappingController.complete.url).withSession(("mappingArn", "TARN0000001"))
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("connectionComplete.title"))
      resultBody should include(htmlEscapedMessage("button.repeatProcess"))
      resultBody should include(htmlEscapedMessage("link.finishSignOut"))
    }

    "InternalServerError due no ARN found after mapping complete" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, routes.MappingController.complete.url)
      an[InternalServerException] shouldBe thrownBy(callEndpointWith(request))
    }
  }

  "not enrolled " should {
    "contain a message indicating that the user is not enrolled for a valid non-mtd enrolment" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(GET, routes.MappingController.notEnrolled.url)
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      bodyOf(result) should include(htmlEscapedMessage("notEnrolled.p1"))
    }
  }

  "already mapped " should {
    "contain a message indicating that the user has already mapped all of her non-mtd identifiers" in {
      givenUserIsAuthenticated(eligibleAgent)
      val request = fakeRequest(GET, routes.MappingController.alreadyMapped.url)
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("error.title"))
      resultBody should include(htmlEscapedMessage("alreadyMapped.p1"))
      resultBody should include(htmlEscapedMessage("alreadyMapped.p2"))
      resultBody should include(htmlEscapedMessage("button.startNow"))
    }
  }

  "incorrectAccount" should {
    trait IncorrectAccountFixture {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = fakeRequest(GET, routes.MappingController.incorrectAccount().url)
      val result = callEndpointWith(request)
      val resultBody: String = bodyOf(result)
    }

    "contain a Try Again button for signing in again and repeating the journey" in new IncorrectAccountFixture {
      resultBody should include(htmlEscapedMessage("button.tryAgain"))
      resultBody should include(""" href="/agent-mapping/signed-out-redirect" """)
    }

    "contain a link to Agent Services Account homepage" in new IncorrectAccountFixture {
      resultBody should include(htmlEscapedMessage("link.goToASAccount"))
      resultBody should include(""" href="http://localhost:9401/agent-services-account" """)
    }

    "return 200 response and contain appropriate content" in new IncorrectAccountFixture {
      status(result) shouldBe 200
      resultBody should include(htmlEscapedMessage("error.title"))
      resultBody should include(htmlEscapedMessage("incorrectAccount.p1"))
      resultBody should include(htmlEscapedMessage("incorrectAccount.p2"))
    }
  }
}