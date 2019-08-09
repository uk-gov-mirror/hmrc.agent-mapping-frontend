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

package uk.gov.hmrc.agentmappingfrontend.connectors

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.Json
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmappingfrontend.model.{AuthProviderId, SubscriptionJourneyRecord}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.encoding.UriPathEncoding.encodePathSegment

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentSubscriptionConnector @Inject()(
  @Named("agent-subscription-baseUrl") baseUrl: URL,
  http: HttpGet with HttpPost,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getSubscriptionJourneyRecord(authProviderId: AuthProviderId)(
    implicit hc: HeaderCarrier): Future[Option[SubscriptionJourneyRecord]] =
    monitor("ConsumedAPI-Agent-Subscription-getSubscriptionJourneyRecord-GET") {
      val url = new URL(baseUrl, s"/agent-subscription/subscription/journey/id/${encodePathSegment(authProviderId.id)}")
      http
        .GET[HttpResponse](url.toString)
        .map(response =>
          response.status match {
            case 200 => Some(Json.parse(response.body).as[SubscriptionJourneyRecord])
            case 204 => None
        })
    }

  def getSubscriptionJourneyRecord(continueId: String)(
    implicit hc: HeaderCarrier): Future[Option[SubscriptionJourneyRecord]] =
    monitor("ConsumedAPI-Agent-Subscription-findByContinueId-GET") {
      val path = s"/agent-subscription/subscription/journey/continueId/${encodePathSegment(continueId)}"
      http
        .GET[HttpResponse](new URL(baseUrl, path).toString)
        .map(response =>
          response.status match {
            case 200 => Some(Json.parse(response.body).as[SubscriptionJourneyRecord])
            case 204 => None
        })
    }

  def createOrUpdateJourney(subscriptionJourneyRecord: SubscriptionJourneyRecord)(
    implicit hc: HeaderCarrier): Future[Unit] =
    monitor("ConsumedAPI-Agent-Subscription-createOrUpdate-POST") {
      val path =
        s"/agent-subscription/subscription/journey/primaryId/${encodePathSegment(subscriptionJourneyRecord.authProviderId.id)}"
      http
        .POST[SubscriptionJourneyRecord, HttpResponse](new URL(baseUrl, path).toString, subscriptionJourneyRecord)
        .map(response =>
          response.status match {
            case 204    => ()
            case status => throw new RuntimeException(s"POST to $path returned $status")
        })
    }

}
