package uk.gov.hmrc.agentmappingfrontend.connectors

import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.model.AuthProviderId
import uk.gov.hmrc.agentmappingfrontend.stubs.AgentSubscriptionStubs
import uk.gov.hmrc.agentmappingfrontend.support.{MetricTestSupport, SubscriptionJourneyRecordSamples}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class AgentSubscriptionConnectorISpec extends BaseControllerISpec with MetricTestSupport with AgentSubscriptionStubs with SubscriptionJourneyRecordSamples {

  private lazy implicit val metrics = app.injector.instanceOf[Metrics]
  private lazy val http = app.injector.instanceOf[HttpClient]
  private lazy val appConfig = app.injector.instanceOf[AppConfig]

  private lazy val connector: AgentSubscriptionConnector = new AgentSubscriptionConnector(http,metrics, appConfig)
  private implicit val hc = HeaderCarrier()


  private val authProviderId = AuthProviderId("12345-credId")

  "getSubscriptionJourneyRecord - by authId" should {

    "return the SubscriptionJourneyRecord with status 200 if found" in {
      givenCleanMetricRegistry()
      givenSubscriptionJourneyRecordExistsForAuthProviderId(authProviderId,sjrNoContinueId)
      await(connector.getSubscriptionJourneyRecord(authProviderId)) shouldBe Some(sjrNoContinueId)
      timerShouldExistsAndBeenUpdated("ConsumedAPI-Agent-Subscription-getSubscriptionJourneyRecord-GET")
    }

    "return None if not found" in {
      givenCleanMetricRegistry()
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(authProviderId)
      await(connector.getSubscriptionJourneyRecord(authProviderId)) shouldBe None
      timerShouldExistsAndBeenUpdated("ConsumedAPI-Agent-Subscription-getSubscriptionJourneyRecord-GET")

    }
  }

  "getSubscriptionJourneyRecord - by continueId" should {

    "return the SubscriptionJourneyRecord with status 200 if found" in {
      givenCleanMetricRegistry()
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id",sjrWithMapping)
      await(connector.getSubscriptionJourneyRecord("continue-id")) shouldBe Some(sjrWithMapping)
      timerShouldExistsAndBeenUpdated("ConsumedAPI-Agent-Subscription-findByContinueId-GET")
    }

    "return None if not found" in {
      givenCleanMetricRegistry()
      givenNoSubscriptionJourneyRecordFoundForContinueId("continue-id")
      await(connector.getSubscriptionJourneyRecord("continue-id")) shouldBe None
      timerShouldExistsAndBeenUpdated("ConsumedAPI-Agent-Subscription-findByContinueId-GET")

    }
  }

  "createOrUpdateJourney" should {
    "return Right(()) if the update was successful" in {
      givenCleanMetricRegistry()
      givenUpdateSubscriptionJourneyRecordSucceeds(sjrWithMapping)
      await(connector.createOrUpdateJourney(sjrWithMapping)) shouldBe Right(())
      timerShouldExistsAndBeenUpdated("ConsumedAPI-Agent-Subscription-createOrUpdate-POST")
    }

    "return Left[String] if the update was unsuccessful" in {
      givenCleanMetricRegistry()
      givenUpdateSubscriptionJourneyRecordFails(sjrWithMapping)

     await(connector.createOrUpdateJourney(sjrWithMapping)).isLeft shouldBe true
      timerShouldExistsAndBeenUpdated("ConsumedAPI-Agent-Subscription-createOrUpdate-POST")
    }
  }

}
