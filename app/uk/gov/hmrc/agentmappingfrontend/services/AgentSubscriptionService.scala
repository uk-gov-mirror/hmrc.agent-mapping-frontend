/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.services

import javax.inject.Inject
import play.api.mvc.Result
import uk.gov.hmrc.agentmappingfrontend.auth.Agent
import uk.gov.hmrc.agentmappingfrontend.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentmappingfrontend.model.SubscriptionJourneyRecord
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AgentSubscriptionService @Inject()(agentSubscriptionConnector: AgentSubscriptionConnector) {

  def createOrUpdateRecordOrFail(agent: Agent, newSjr: SubscriptionJourneyRecord, onSuccess: => Future[Result])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    agentSubscriptionConnector.createOrUpdateJourney(newSjr).flatMap {
      case Right(_) => onSuccess
      case Left(e) =>
        throw new RuntimeException(
          s"update subscriptionJourneyRecord call failed $e for agentCode ${agent.agentCodeOpt.getOrElse(" ")}")
    }

}
