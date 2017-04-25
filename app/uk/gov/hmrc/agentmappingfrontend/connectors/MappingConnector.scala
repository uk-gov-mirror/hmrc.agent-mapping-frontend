/*
 * Copyright 2017 HM Revenue & Customs
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
import javax.inject.{Inject, Named, Singleton}

import play.api.http.Status
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPut, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MappingConnector @Inject()(@Named("agent-mapping-baseUrl") baseUrl: URL, http: HttpPut) {
  def createMapping(utr: Utr, arn: Arn, saAgentReference: SaAgentReference)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    http.PUT(createUrl(utr, arn, saAgentReference), "").map{
      r => r.status
    }.recover {
      case e: Upstream4xxResponse if Status.FORBIDDEN.equals(e.upstreamResponseCode) => Status.FORBIDDEN
      case e => throw e
    }
  }

  private def createUrl(utr: Utr, arn: Arn, saAgentReference: SaAgentReference): String = {
    new URL(baseUrl, s"/agent-mapping/mappings/${utr.value}/${arn.value}/$saAgentReference").toString
  }
}
