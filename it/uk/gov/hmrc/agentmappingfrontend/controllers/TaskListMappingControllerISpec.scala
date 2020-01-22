package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.config.FrontendAppConfig
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.repository.TaskListMappingRepository
import uk.gov.hmrc.agentmappingfrontend.stubs.{AgentSubscriptionStubs, AuthStubs, MappingStubs}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.{agentNotEnrolled, mtdAsAgent, vatEnrolledAgent}
import uk.gov.hmrc.agentmappingfrontend.support.SubscriptionJourneyRecordSamples
import uk.gov.hmrc.domain.AgentCode

import scala.concurrent.ExecutionContext.Implicits.global

class TaskListMappingControllerISpec extends BaseControllerISpec with AuthStubs with AgentSubscriptionStubs with SubscriptionJourneyRecordSamples {

  private lazy val repo = app.injector.instanceOf[TaskListMappingRepository]

  private lazy val controller = app.injector.instanceOf[TaskListMappingController]

  private lazy val appConfig = app.injector.instanceOf[FrontendAppConfig]

  val mappingStubs = MappingStubs

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "context root" should {
    "redirect to the /agent-mapping/task-list/start page" in {
      val request = FakeRequest(GET, "/agent-mapping/task-list/?continueId=continue-id")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).head should include("/task-list/start")
    }

    "400 BadRequest if there is no query param (continueId) in url" in {
      val request = FakeRequest(GET, "/agent-mapping/task-list/")
      val result = callEndpointWith(request)
      status(result) shouldBe 400
    }
  }

  "task-list/start" should {
    "303 to /task-list/error/incorrect-account if user has HMRC-AS-AGENT" in {
      givenUserIsAuthenticated(mtdAsAgent)
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithNoUserMappings)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start/?continueId=continue-id")
      val result = callEndpointWith(request)
      val id = await(repo.findByContinueId("continue-id").get.id)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.incorrectAccount(id).url)
    }

    "throw RuntimeException if subscription journey record does not exist for that user" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForContinueId("continue-id")
      val request = FakeRequest(GET, "/agent-mapping/task-list/start/?continueId=continue-id")

      intercept[RuntimeException] {
        await(controller.start("continue-id")(request))
      }.getMessage should be("continueId continue-id not recognised")
    }

    "400 BadRequest if url is missing a continueId" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start/")
      val result = callEndpointWith(request)
      status(result) shouldBe 400
    }

    "200 the start page if the user enters mapping for the first time" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithNoUserMappings)
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithNoUserMappings)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start/?continueId=continue-id")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result,
        "start.task-list.heading",
        "start.task-list.need-to-do",
        "start.task-list.need-to-know",
        "start.task-list.need-to-know.panel",
        "button.saveContinue", "button.saveComeBackLater")
      bodyOf(result) should include("/task-list/client-relationships-found?id=")
    }

    "303 to /task-list/client-relationships-found if there already exists some mappings but the current user has not yet mapped" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithMapping)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start/?continueId=continue-id")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      val id = await(repo.findByContinueId("continue-id")).get.id

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showClientRelationshipsFound(id).url)
    }

    "303 to /task-list/existing-client-relationships if the current user has already mapped" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithUserAlreadyMapped)
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithUserAlreadyMapped)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start/?continueId=continue-id")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      val id = await(repo.findByContinueId("continue-id")).get.id

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showExistingClientRelationships(id).url)
    }
  }

  "task-list/show-client-relationships" should {

    "200 the show client relationships; get client count and update db when the current user first visits the page" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
      val id = await(repo.create("continue-id"))
      mappingStubs.givenClientCountRecordsFound(4)
      val request = FakeRequest(GET, s"/agent-mapping/task-list/client-relationships-found/?id=$id")
      val result = callEndpointWith(request)
      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(result,
        "clientRelationshipsFound.title","clientRelationshipsFound.multi.title",
        "clientRelationshipsFound.multi.p1",
        "clientRelationshipsFound.multi.td",
        "clientRelationshipsFound.multi.p2",
      "button.saveContinue", "button.saveComeBackLater")

      bodyOf(result) should include(appConfig.agentSubscriptionFrontendProgressSavedUrl)
      bodyOf(result) should include(routes.TaskListMappingController.showGGTag(id).url)

    }

    "200 the show client relationships without update to db if the user has come from the next page (not the first time on page)" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 12, alreadyMapped = true), "continue-id"))
      val request = FakeRequest(GET, s"/agent-mapping/task-list/client-relationships-found/?id=$id")
      val result = callEndpointWith(request)
      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(result,
        "clientRelationshipsFound.title","clientRelationshipsFound.multi.title",
        "clientRelationshipsFound.multi.p1",
        "clientRelationshipsFound.multi.td",
        "clientRelationshipsFound.multi.p2",
        "button.saveContinue", "button.saveComeBackLater")

      bodyOf(result) should include(appConfig.agentSubscriptionFrontendProgressSavedUrl)
      bodyOf(result) should include(routes.TaskListMappingController.showGGTag(id).url)
    }

    "200 the show client relationships with max record text if the client count exceeds config max (set here at 15)" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 16, alreadyMapped = true), "continue-id"))
      val request = FakeRequest(GET, s"/agent-mapping/task-list/client-relationships-found/?id=$id")
      val result = callEndpointWith(request)
      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(result,
        "clientRelationshipsFound.title","clientRelationshipsFound.multi.title",
        "clientRelationshipsFound.multi.p1",
        "clientRelationshipsFound.max",
        "clientRelationshipsFound.multi.p2",
        "button.saveContinue", "button.saveComeBackLater")

      bodyOf(result) should include(appConfig.agentSubscriptionFrontendProgressSavedUrl)
      bodyOf(result) should include(routes.TaskListMappingController.showGGTag(id).url)
    }

    "throw RuntimeException if there was no task list mapping record found (for example if the user manually entered the url from /agent-subscription" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))

      intercept[RuntimeException] {
        await(controller.showClientRelationshipsFound("SOMETHING")(FakeRequest()))
      }.getMessage should be("no task-list mapping record found for agent code HZ1234")
    }
  }

  "GET /task-list/tag-gg" should {
    "display the ggTag page" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/tag-gg/?id=$id")
      val result = callEndpointWith(request)

      checkHtmlResultContainsEscapedMsgs(result, "gg-tag.title",
        "gg-tag.p1",
        "gg-tag.form.identifier",
        "gg-tag.form.hint",
        "gg-tag.xs")
    }
  }

  "POST /task-list/tag-gg" should {
    "redirect to existing-client-relationships and update sjr to store gg-tag when a valid gg-tag is submitted" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithNoUserMappings)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 12), "continue-id"))
      givenUpdateSubscriptionJourneyRecordSucceeds(sjrWithNoUserMappings
        .copy(
          userMappings = UserMapping(
            AuthProviderId("12345-credId"),
            agentCode = Some(AgentCode("HZ1234")),
            count = 12,
            legacyEnrolments =  Seq(AgentEnrolment(AgentRefNo, IdentifierValue("HZ1234"))),
            ggTag= "1234") :: sjrWithNoUserMappings.userMappings))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/tag-gg/?id=$id").withFormUrlEncodedBody(
        "ggTag" -> "1234", "continue" -> "continue"
      )
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showExistingClientRelationships(id).url)
    }

    "redisplay the page with errors when an invalid gg-tag is submitted" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/tag-gg/?id=$id").withFormUrlEncodedBody(
        "ggTag" -> "ab!7", "continue" -> "continue"
      )
      val result = callEndpointWith(request)

      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "gg-tag.title", "error.gg-tag.invalid")
    }

    "throw a runtime exception when there is no journey record found for continueId" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
      givenSubscriptionJourneyRecordNotFoundForContinueId("continue-id")
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 12), "continue-id"))
      givenUpdateSubscriptionJourneyRecordSucceeds(sjrWithNoUserMappings
        .copy(
          userMappings = UserMapping(
            AuthProviderId("12345-credId"),
            agentCode = Some(AgentCode("HZ1234")),
            count = 12,
            legacyEnrolments =  Seq(AgentEnrolment(AgentRefNo, IdentifierValue("HZ1234"))),
            ggTag= "1234") :: sjrWithNoUserMappings.userMappings))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/tag-gg/?id=$id").withFormUrlEncodedBody(
        "ggTag" -> "1234", "continue" -> "continue"
      )
      intercept[RuntimeException] {
        callEndpointWith(request)
      }.getMessage shouldBe "no subscription journey record found when submitting gg tag for agent code HZ1234"
    }

    "throw a runtime exception when there is no mapping record found" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
      givenSubscriptionJourneyRecordNotFoundForContinueId("continue-id")

      val request = FakeRequest(POST, s"/agent-mapping/task-list/tag-gg/?id=foo").withFormUrlEncodedBody(
        "ggTag" -> "1234", "continue" -> "continue"
      )

      intercept[RuntimeException] {
        callEndpointWith(request)
      }.getMessage shouldBe "no task-list mapping record found for agent code HZ1234"
    }
  }

  "GET /task-list/existing-client-relationships" should {

    "200 the existing-client-relationships page with back link to /show-client-relationships-found if already mapped" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 1, alreadyMapped = true), "continue-id"))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/existing-client-relationships/?id=$id")
      val result = callEndpointWith(request)
      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(result,
        "existingClientRelationships.title",
      "existingClientRelationships.heading",
      "existingClientRelationships.p1",
      "existingClientRelationships.yes",
      "existingClientRelationships.no")

      //bodyOf(result) should include(htmlEscapedMessage("existingClientRelationships.td", "6666"))
      bodyOf(result) should include(htmlEscapedMessage("copied.table.single.th", 1))
      bodyOf(result) should include(routes.TaskListMappingController.showGGTag(id).url)

      result should containSubmitButton("button.saveContinue","existing-client-relationships-continue")
      result should containSubmitButton("button.saveComeBackLater","existing-client-relationships-save")
    }

    "200 the existing-client-relationships page with back link to /agent-subscription/task-list if not already mapped (has just arrived from agent-subscription/task-list" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 1, alreadyMapped = false), "continue-id"))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/existing-client-relationships/?id=$id")
      val result = callEndpointWith(request)
      status(result) shouldBe 200

      bodyOf(result) should include(appConfig.agentSubscriptionFrontendTaskListUrl)
    }
  }

  "POST /task-list/existing-client-relationships" should {

    "redirect to agent-subscription/return-after-mapping if user selects 'No' and continues" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 1, alreadyMapped = true), "continue-id"))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/existing-client-relationships/?id=$id").withFormUrlEncodedBody(
        "additional-clients" -> "no", "continue" -> "continue"
      )

      val result = callEndpointWith(request)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.SignedOutController.returnAfterMapping().url)
    }

    "redirect to gg/sign-in if user selects 'Yes' and continues" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 1, alreadyMapped = true), "continue-id"))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/existing-client-relationships/?id=$id").withFormUrlEncodedBody(
        "additional-clients" -> "yes", "continue" -> "continue"
      )

      val result = callEndpointWith(request)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.SignedOutController.taskListSignOutAndRedirect(id).url)
    }

    "redirect to agent-subscription/saved-progress if user selects 'Yes' and saves" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 1, alreadyMapped = true), "continue-id"))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/existing-client-relationships/?id=$id").withFormUrlEncodedBody(
        "additional-clients" -> "yes", "continue" -> "save"
      )

      val result = callEndpointWith(request)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(s"${appConfig.agentSubscriptionFrontendProgressSavedUrl}/task-list/existing-client-relationships/?id=$id")
    }

    "redirect to agent-subscription/saved-progress if user selects 'No' and saves" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 1, alreadyMapped = true), "continue-id"))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/existing-client-relationships/?id=$id").withFormUrlEncodedBody(
        "additional-clients" -> "no", "continue" -> "save"
      )

      val result = callEndpointWith(request)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(s"${appConfig.agentSubscriptionFrontendProgressSavedUrl}/task-list/existing-client-relationships/?id=$id")
    }

    "redisplay the page with errors if the form is invalid" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 1, alreadyMapped = true), "continue-id"))

      val request = FakeRequest(POST, s"/agent-mapping/task-list/existing-client-relationships/?id=$id").withFormUrlEncodedBody(
        "additional-clients" -> "foo", "continue" -> "save"
      )

      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "existingClientRelationships.title", "error.existingClientRelationships.choice.invalid")
    }
  }

  "return-from GG login" should {
    "redirect to /client-relationships-found when user has not mapped before " in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordNotFoundForAuthProviderId(AuthProviderId("12345-credId"))
      val id = await(repo.create("continue-id"))
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithMapping.copy(authProviderId = AuthProviderId("123-credId")))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/start-submit/?id=$id")
      val result = callEndpointWith(request)
      val newId = await(repo.findByContinueId("continue-id").get.id)

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showClientRelationshipsFound(newId).url)
    }

    "303 to /already-mapped page when the user has already mapped" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      givenSubscriptionJourneyRecordExistsForContinueId(
        "continue-id",
        sjrWithMapping.copy(userMappings = UserMapping(
          AuthProviderId("12345-credId"),
          None,
          legacyEnrolments = List.empty,
          ggTag="") :: sjrWithMapping.userMappings)
      )

      val request = FakeRequest(GET, s"/agent-mapping/task-list/start-submit/?id=$id")
      val result = callEndpointWith(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.alreadyMapped(id).url)
    }

    "throw a runtime exception when there is no mapping record" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordNotFoundForAuthProviderId(AuthProviderId("12345-credId"))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/start-submit/?id=foo")
      intercept[RuntimeException] {
        callEndpointWith(request)
      }.getMessage should startWith("no task-list mapping record")
    }

    "throw a runtime exception when there is no subscription journey record" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordNotFoundForAuthProviderId(AuthProviderId("12345-credId"))
      val id = await(repo.create("continue-id"))
      givenNoSubscriptionJourneyRecordFoundForContinueId("continue-id")

      val request = FakeRequest(GET, s"/agent-mapping/task-list/start-submit/?id=$id")
      intercept[RuntimeException] {
        callEndpointWith(request)
      }.getMessage should startWith("no subscription journey record found")

    }

    "redirect to not enrolled if the logged in user has no enrolments" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      givenSubscriptionJourneyRecordNotFoundForAuthProviderId(AuthProviderId("12345-credId"))
      val id = await(repo.create("continue-id"))
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithMapping.copy(authProviderId = AuthProviderId("123-credId")))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/start-submit/?id=$id")
      val result = callEndpointWith(request)
      val newId = await(repo.findByContinueId("continue-id").get.id)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.notEnrolled(newId).url)
    }
  }

  "200 /task-list/error/incorrect-account" should {
    "display the correct content" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = FakeRequest(GET, s"/agent-mapping/task-list/error/incorrect-account/?id=SOMETHING")
      val result = callEndpointWith(request)

      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(result,
        "incorrectAccount.h1", "incorrectAccount.p1")

    }
  }

  "200 /task-list/already-linked" should {
    "display the correct content" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val request = FakeRequest(GET, s"/agent-mapping/task-list/error/already-linked/?id=SOMETHING")
      val result = callEndpointWith(request)

      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(result,
        "alreadyMapped.h1", "alreadyMapped.p1")
    }
  }

  "200 /task-list/not-enrolled" should {
    "display the not enrolled page" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = FakeRequest(GET, s"/agent-mapping/task-list/error/not-enrolled/?id=SOMETHING")
      val result = callEndpointWith(request)

      status(result) shouldBe 200

      checkHtmlResultContainsEscapedMsgs(result,
        "notEnrolled.h1", "notEnrolled.p1", "notEnrolled.p2")
    }
  }
}
