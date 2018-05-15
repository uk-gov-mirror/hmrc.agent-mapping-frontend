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

package uk.gov.hmrc.agentmappingfrontend.audit

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.agentmappingfrontend.audit.AgentFrontendMappingEvent.AgentFrontendMappingEvent
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.Try

case class AuditData(activeEnrolments: Set[String],
                     eligible: Boolean,
                     creds: Credentials)

object AuditService {

  def auditCheckAgentRefCodeEvent(auditService: AuditService)(
      auditData: AuditData)(implicit hc: HeaderCarrier,
                            request: Request[Any]): Unit = {
    val event = createEvent(
      AgentFrontendMappingEvent.CheckAgentRefCode,
      "check-agent-ref-code",
      Seq(
        "eligible" -> auditData.eligible,
        "activeEnrolments" -> auditData.activeEnrolments.mkString(","),
        "authProviderId" -> auditData.creds.providerId,
        "authProviderType" -> auditData.creds.providerType
      )
    )
    auditService.send(event)
  }

  private def createEvent(event: AgentFrontendMappingEvent,
                          transactionName: String,
                          details: Seq[(String, Any)])(
      implicit hc: HeaderCarrier,
      request: Request[Any]): DataEvent = {
    DataEvent(
      auditSource = "agent-mapping-frontend",
      auditType = event.toString,
      tags = hc.toAuditTags(transactionName, request.path),
      detail =
        hc.toAuditDetails(details.map(pair => pair._1 -> pair._2.toString): _*)
    )
  }

}

trait AuditService {
  def send(event: DataEvent)(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class AuditServiceImpl @Inject()(val auditConnector: AuditConnector)
    extends AuditService {

  override def send(event: DataEvent)(
      implicit hc: HeaderCarrier): Future[Unit] = {
    Future {
      Try(auditConnector.sendEvent(event))
    }
  }
}

object AgentFrontendMappingEvent extends Enumeration {
  type AgentFrontendMappingEvent = Value
  val CheckAgentRefCode = Value
}
