package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.http.Writeable
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.config.FrontendAppConfig
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.repository.TaskListMappingRepository
import uk.gov.hmrc.agentmappingfrontend.stubs.{AgentSubscriptionStubs, AuthStubs, MappingStubs}
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.{mtdAsAgent, vatEnrolledAgent}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.AgentCode

import scala.concurrent.ExecutionContext.Implicits.global

class TaskListMappingControllerISpec extends BaseControllerISpec with AuthStubs with AgentSubscriptionStubs {

  private lazy val repo = app.injector.instanceOf[TaskListMappingRepository]

  private lazy val controller = app.injector.instanceOf[TaskListMappingController]

  private lazy val appConfig = app.injector.instanceOf[FrontendAppConfig]

  val mappingStubs = MappingStubs

  val businessDetails = BusinessDetails(BusinessType.LimitedCompany,Utr("2000000000"),Postcode("AA11AA"),None,None,None,None)

  def sjrBuilder(
                  authProviderId: String,
                  continueId: Option[String] = None,
                  userMappings: List[UserMapping] = List.empty,
                  mappingComplete: Boolean = false, cleanCredId: Option[AuthProviderId] = None) = SubscriptionJourneyRecord(
    authProviderId = AuthProviderId(authProviderId),
    continueId = continueId,
    businessDetails = businessDetails,
    amlsData = None,
    userMappings = userMappings,
    mappingComplete = mappingComplete,
    cleanCredsAuthProviderId = cleanCredId,
    lastModifiedDate = None)

  val sjrNoContinueId = sjrBuilder("12345-credId")
  val sjrWithNoUserMappings = sjrNoContinueId.copy(continueId = Some("continue-id"))
  val sjrWithCleanCredId = sjrWithNoUserMappings.copy(cleanCredsAuthProviderId = Some(AuthProviderId("12345-credId")))
  val sjrWithMapping = sjrWithNoUserMappings.copy(userMappings =
    List(
    UserMapping(
      authProviderId = AuthProviderId("1-credId"),
      agentCodes = List(AgentCode("agentCode-1")),
      count = 1,
      ggTag = "")))

  val sjrWithUserAlreadyMapped = sjrWithNoUserMappings.copy(userMappings =
    List(
      UserMapping(
        authProviderId = AuthProviderId("12345-credId"),
        agentCodes = List(AgentCode("agentCode-1")),
        count = 1,
        ggTag = "")))

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "context root" should {
    "redirect to the /agent-mapping/task-list/start page" in {
      val request = FakeRequest(GET, "/agent-mapping/task-list/")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result).head should include("/task-list/start")
    }
  }

  "task-list/start" should {
    "303 to agent services if user has HMRC-AS-AGENT" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(appConfig.agentServicesFrontendExternalUrl)
    }

    "throw RuntimeException if subscription journey record does not exist for that user" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
      val request = FakeRequest(GET, "/agent-mapping/task-list/start")

      intercept[RuntimeException] {
        await(controller.start(request))
      }.getMessage should be("mandatory subscription journey record was missing for authProviderID AuthProviderId(12345-credId)")
    }

    "throw RuntimeException if subscription journey record exists but is missing a continueId" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrNoContinueId)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start")

      intercept[RuntimeException] {
        await(controller.start(request))
      }.getMessage should be("continueId not found in agent subscription record")
    }

    "200 the start page" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithNoUserMappings)
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithNoUserMappings)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result,
        "connectAgentServices.start.whatYouNeedToKnow.heading",
        "connectAgentServices.start.whatYouNeedToKnow.p1",
        "connectAgentServices.start.inset",
        "connectAgentServices.start.whatYouNeedToDo.heading",
        "connectAgentServices.start.whatYouNeedToDo.p1",
        "connectAgentServices.start.whatYouNeedToDo.p2",
        "button.continue")
      checkHtmlResultContainsMsgs(result, "connectAgentServices.start.whatYouNeedToKnow.p1")
      bodyOf(result) should include("/task-list/client-relationships-found?id=")
    }

    "200 the start sign in required if the user is logged in with the clean cred id" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithCleanCredId)
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithNoUserMappings)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 200
      checkHtmlResultContainsEscapedMsgs(result, "start.not-signed-in.title")
    }

    "303 to /task-list/client-relationships-found if there already exists some mappings" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithMapping)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      val id = await(repo.findByContinueId("continue-id")).get.id

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showClientRelationshipsFound(id).url)
    }

    "303 to /task-list/existing-client-relationships if the current user has already mapped" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithUserAlreadyMapped)
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithUserAlreadyMapped)
      val request = FakeRequest(GET, "/agent-mapping/task-list/start")
      val result = callEndpointWith(request)
      status(result) shouldBe 303
      val id = await(repo.findByContinueId("continue-id")).get.id

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showExistingClientRelationships(id).url)
    }
  }

  "task-list/show-client-relationships" should {

    "200 the show client relationships; get client count and update db when the current user first visits the page" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      mappingStubs.givenClientCountRecordsFound(12)
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
      bodyOf(result) should include(routes.TaskListMappingController.confirmClientRelationshipsFound(id).url)

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
      bodyOf(result) should include(routes.TaskListMappingController.confirmClientRelationshipsFound(id).url)
    }

    "303 /task-list/start if there was no task list mapping record found (for example if the user manually entered the url from /agent-subscription" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)

      val request = FakeRequest(GET, s"/agent-mapping/task-list/client-relationships-found/?id=SOMETHING")
      val result = callEndpointWith(request)
      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.start().url)
    }
  }

  "task-list/confirm-client-relationships" should {
    "303 to the existing client relationships with update db when the current user first visits the page" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithNoUserMappings)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 12), "continue-id"))
      givenUpdateSubscriptionJourneyRecordSucceeds(sjrWithNoUserMappings
        .copy(
          userMappings = UserMapping(AuthProviderId("12345-credId"),agentCodes = Seq(AgentCode("HZ1234")),count = 12, ggTag= "") :: sjrWithNoUserMappings.userMappings))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/confirm-client-relationships-found/?id=$id")
      val result = callEndpointWith(request)
      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showExistingClientRelationships(id).url)
      println(await(repo.findRecord(id).get))

      await(repo.findRecord(id).get).alreadyMapped shouldBe true
    }

    "303 to the existing client relationships with no update to db when the current user has already mapped" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      val record = await(repo.findRecord(id)).get
      await(repo.upsert(record.copy(clientCount = 12, alreadyMapped = true), "continue-id"))
      val request = FakeRequest(GET, s"/agent-mapping/task-list/confirm-client-relationships-found/?id=$id")
      val result = callEndpointWith(request)
      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.showExistingClientRelationships(id).url)
    }

    "303 to task-list/start when no task list mapping record found (for example when entering the url manually from agent-subscription/task-list)" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val request = FakeRequest(GET, s"/agent-mapping/task-list/confirm-client-relationships-found/?id=SOMETHING")
      val result = callEndpointWith(request)
      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.TaskListMappingController.start().url)
    }

    "throw a RuntimeException if no subscription journey record is found by continue id" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      val id = repo.create("continue-id")
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(AuthProviderId("12345-credId"))
        //givenNoSubscriptionJourneyRecordFoundForContinueId("continue-id")

      val request = FakeRequest(GET, s"/agent-mapping/task-list/confirm-client-relationships-found/?id=$id")
      /*intercept[RuntimeException] {
        await(controller.start(request))
      }.getMessage should be("continueId not found in agent subscription record")*/

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
          "existingClientRelationships.single.th",

      "existingClientRelationships.td",
      "existingClientRelationships.heading",
      "existingClientRelationships.p1",
      "existingClientRelationships.yes",
      "existingClientRelationships.no")

      bodyOf(result) should include(routes.TaskListMappingController.showClientRelationshipsFound(id).url)

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

      bodyOf(result) should include(s"${appConfig.agentSubscriptionFrontendExternalUrl}${appConfig.agentSubscriptionFrontendTaskListPath}")
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

      redirectLocation(result) shouldBe Some(appConfig.agentSubscriptionFrontendProgressSavedUrl)
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

      redirectLocation(result) shouldBe Some(appConfig.agentSubscriptionFrontendProgressSavedUrl)
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

    "200 to /already-mapped page when the user has already mapped" in {
      givenUserIsAuthenticated(vatEnrolledAgent)
      givenSubscriptionJourneyRecordExistsForAuthProviderId(AuthProviderId("12345-credId"), sjrWithMapping)
      val id = await(repo.create("continue-id"))
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithMapping.copy(userMappings = UserMapping(AuthProviderId("12345-credId"),ggTag="") :: sjrWithMapping.userMappings))

      val request = FakeRequest(GET, s"/agent-mapping/task-list/start-submit/?id=$id")
      val result = callEndpointWith(request)

      status(result) shouldBe 200
    }
  }

}
