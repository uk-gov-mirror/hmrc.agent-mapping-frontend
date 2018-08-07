/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmappingfrontend.model.{SaMapping, VatMapping}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MappingConnector @Inject()(
  @Named("agent-mapping-baseUrl") baseUrl: URL,
  httpGet: HttpGet,
  httpPut: HttpPut,
  httpDelete: HttpDelete,
  metrics: Metrics
) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def createMapping(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] =
    monitor(s"ConsumedAPI-Mapping-CreateMapping-PUT") {
      httpPut
        .PUT(createUrl(arn), "")
        .map { r =>
          r.status
        }
        .recover {
          case e: Upstream4xxResponse if Status.FORBIDDEN.equals(e.upstreamResponseCode) => Status.FORBIDDEN
          case e: Upstream4xxResponse if Status.CONFLICT.equals(e.upstreamResponseCode)  => Status.CONFLICT
          case e                                                                         => throw e
        }
    }

  private def createUrl(arn: Arn): String =
    new URL(baseUrl, s"/agent-mapping/mappings/arn/${arn.value}").toString

  private def deleteUrl(arn: Arn): String =
    new URL(baseUrl, s"/agent-mapping/test-only/mappings/${arn.value}").toString

  private def findSaUrl(arn: Arn): String =
    new URL(baseUrl, s"agent-mapping/mappings/sa/${arn.value}").toString

  private def findVatUrl(arn: Arn): String =
    new URL(baseUrl, s"agent-mapping/mappings/vat/${arn.value}").toString

  def findSaMappingsFor(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[SaMapping]] =
    monitor(s"ConsumedAPI-Mapping-FindSaMappingsForArn-GET") {
      httpGet.GET[JsValue](findSaUrl(arn)).map { response =>
        (response \ "mappings").as[Seq[SaMapping]]
      } recover {
        case _: NotFoundException => Seq.empty
        case ex: Throwable        => throw new RuntimeException(ex)
      }
    }

  def findVatMappingsFor(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[VatMapping]] =
    monitor(s"ConsumedAPI-Mapping-FindVatMappingsForArn-GET") {
      httpGet.GET[JsValue](findVatUrl(arn)).map { response =>
        (response \ "mappings").as[Seq[VatMapping]]
      } recover {
        case _: NotFoundException => Seq.empty
        case ex: Throwable        => throw new RuntimeException(ex)
      }
    }

  def deleteAllMappingsBy(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] =
    monitor(s"ConsumedAPI-Mapping-DeleteAllMappingsByArn-DELETE") {
      httpDelete.DELETE(deleteUrl(arn)).map { r =>
        r.status
      }
    }
}
