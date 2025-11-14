/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.model.MappingDetailsRepositoryRecord
import uk.gov.hmrc.agentmappingfrontend.model.MappingDetailsRequest
import uk.gov.hmrc.agentmappingfrontend.model.SaMapping
import uk.gov.hmrc.agentmappingfrontend.util.RequestSupport.hc
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class MappingConnector @Inject() (
  http: HttpClientV2,
  val metrics: Metrics,
  appConfig: AppConfig
)(implicit
  val ec: ExecutionContext
)
extends Logging {

  def createMapping(arn: Arn)(implicit rh: RequestHeader): Future[Int] = http
    .put(url"$baseUrl/agent-mapping/mappings/arn/${arn.value}")
    .execute[HttpResponse]
    .map(_.status)

  def getClientCount(implicit rh: RequestHeader): Future[Int] = http
    .get(url"$baseUrl/agent-mapping/client-count")
    .execute[HttpResponse]
    .map { response =>
      (response.json \ "clientCount").as[Int]
    }

  def findSaMappingsFor(arn: Arn)(implicit rh: RequestHeader): Future[Seq[SaMapping]] = http
    .get(url"$baseUrl/agent-mapping/mappings/sa/${arn.value}")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case OK => (response.json \ "mappings").as[Seq[SaMapping]]
        case NOT_FOUND => Seq.empty
        case s => throw new RuntimeException(s"unexpected error when calling findSaMappingsFor, status: $s")
      }
    }

  def deleteAllMappingsBy(arn: Arn)(implicit rh: RequestHeader): Future[Int] = http
    .delete(url"$baseUrl/agent-mapping/test-only/mappings/${arn.value}")
    .execute[HttpResponse]
    .map(_.status)

  def createOrUpdateMappingDetails(
    arn: Arn,
    mappingDetailsRequest: MappingDetailsRequest
  )(implicit
    rh: RequestHeader
  ): Future[Unit] = http
    .post(url"$baseUrl/agent-mapping/mappings/details/arn/${arn.value}")
    .withBody(Json.toJson(mappingDetailsRequest))
    .execute[HttpResponse]
    .map { r =>
      r.status match {
        case status if is2xx(status) => ()
        case status if status == CONFLICT => throw new ConflictException(s"Failed to create or update mapping details for $arn")
        case status =>
          logger.error(s"status: $status Failed to create or update mapping details for arn: $arn")
          throw new RuntimeException
      }
    }

  def getMappingDetails(
    arn: Arn
  )(implicit rh: RequestHeader): Future[Option[MappingDetailsRepositoryRecord]] = {
    http
      .get(url"$baseUrl/agent-mapping/mappings/details/arn/${arn.value}")
      .execute[Option[MappingDetailsRepositoryRecord]]
  }.recover {
    case _: NotFoundException =>
      logger.warn(s"no mapping details found for this arn: $arn")
      None
    case ex =>
      logger.warn(s"retrieval of mapping details failed for unknown reason...$ex")
      None
  }

  private lazy val baseUrl = appConfig.agentMappingBaseUrl

}
