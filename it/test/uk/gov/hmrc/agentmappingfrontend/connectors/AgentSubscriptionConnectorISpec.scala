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

package uk.gov.hmrc.agentmappingfrontend.connectors

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.model.AuthProviderId
import uk.gov.hmrc.agentmappingfrontend.stubs.AgentSubscriptionStubs
import uk.gov.hmrc.agentmappingfrontend.support.SubscriptionJourneyRecordSamples
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.ExecutionContext.Implicits.global

class AgentSubscriptionConnectorISpec
extends BaseControllerISpec
with AgentSubscriptionStubs
with SubscriptionJourneyRecordSamples {

  private implicit lazy val metrics: Metrics = app.injector.instanceOf[Metrics]
  private lazy val http = app.injector.instanceOf[HttpClientV2]
  private lazy val appConfig = app.injector.instanceOf[AppConfig]

  private lazy val connector: AgentSubscriptionConnector =
    new AgentSubscriptionConnector(
      http,
      metrics,
      appConfig
    )
  private implicit val rh: RequestHeader = FakeRequest()

  private val authProviderId = AuthProviderId("12345-credId")

  "getSubscriptionJourneyRecord - by authId" should {
    "return the SubscriptionJourneyRecord with status 200 if found" in {
      givenSubscriptionJourneyRecordExistsForAuthProviderId(authProviderId, sjrNoContinueId)
      await(connector.getSubscriptionJourneyRecord(authProviderId)) shouldBe Some(sjrNoContinueId)
    }

    "return None if not found" in {
      givenNoSubscriptionJourneyRecordFoundForAuthProviderId(authProviderId)
      await(connector.getSubscriptionJourneyRecord(authProviderId)) shouldBe None
    }
  }

  "getSubscriptionJourneyRecord - by continueId" should {
    "return the SubscriptionJourneyRecord with status 200 if found" in {
      givenSubscriptionJourneyRecordExistsForContinueId("continue-id", sjrWithMapping)
      await(connector.getSubscriptionJourneyRecord("continue-id")) shouldBe Some(sjrWithMapping)
    }

    "return None if not found" in {
      givenNoSubscriptionJourneyRecordFoundForContinueId("continue-id")
      await(connector.getSubscriptionJourneyRecord("continue-id")) shouldBe None
    }
  }

  "createOrUpdateJourney" should {
    "return Right(()) if the update was successful" in {
      givenUpdateSubscriptionJourneyRecordSucceeds(sjrWithMapping)
      await(connector.createOrUpdateJourney(sjrWithMapping)) shouldBe Right(())
    }

    "return Left[String] if the update was unsuccessful" in {
      givenUpdateSubscriptionJourneyRecordFails(sjrWithMapping)
      await(connector.createOrUpdateJourney(sjrWithMapping)).isLeft shouldBe true
    }
  }

}
