/*
 * Copyright 2020 HM Revenue & Customs
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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.model.{AuthProviderId, SubscriptionJourneyRecord}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.encoding.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentSubscriptionConnector @Inject()(
  http: HttpClient,
  metrics: Metrics,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getSubscriptionJourneyRecord(authProviderId: AuthProviderId)(
    implicit hc: HeaderCarrier): Future[Option[SubscriptionJourneyRecord]] = {
    val url =
      s"${appConfig.agentSubscriptionBaseUrl}/agent-subscription/subscription/journey/id/${encodePathSegment(authProviderId.id)}"
    monitor("ConsumedAPI-Agent-Subscription-getSubscriptionJourneyRecord-GET") {
      http
        .GET[HttpResponse](url)
        .map(response => {
          response.status match {
            case 200 => Some(Json.parse(response.body).as[SubscriptionJourneyRecord])
            case 204 => None
          }
        })
    }
  }

  def getSubscriptionJourneyRecord(continueId: String)(
    implicit hc: HeaderCarrier): Future[Option[SubscriptionJourneyRecord]] = {
    val url =
      s"${appConfig.agentSubscriptionBaseUrl}/agent-subscription/subscription/journey/continueId/${encodePathSegment(continueId)}"
    monitor("ConsumedAPI-Agent-Subscription-findByContinueId-GET") {
      http
        .GET[HttpResponse](url)
        .map(response => {

          response.status match {
            case 200 => Some(Json.parse(response.body).as[SubscriptionJourneyRecord])
            case 204 => None
          }
        })
    }
  }

  def createOrUpdateJourney(subscriptionJourneyRecord: SubscriptionJourneyRecord)(
    implicit hc: HeaderCarrier): Future[Either[String, Unit]] = {
    val url =
      s"${appConfig.agentSubscriptionBaseUrl}/agent-subscription/subscription/journey/primaryId/${encodePathSegment(
        subscriptionJourneyRecord.authProviderId.id)}"
    monitor("ConsumedAPI-Agent-Subscription-createOrUpdate-POST") {
      http
        .POST[SubscriptionJourneyRecord, HttpResponse](url, subscriptionJourneyRecord)
        .map(response => {

          response.status match {
            case 204    => Right(())
            case status => Left(s"POST to $url returned $status")
          }
        })
        .recover {
          case ex: Throwable => Left(s"unexpected response ${ex.getMessage}")
        }
    }
  }
}
