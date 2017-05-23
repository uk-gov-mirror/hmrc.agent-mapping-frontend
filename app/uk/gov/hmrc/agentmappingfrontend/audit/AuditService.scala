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

package uk.gov.hmrc.agentmappingfrontend.audit

import javax.inject.{Inject, Singleton}

import play.api.mvc.Request
import uk.gov.hmrc.agentmappingfrontend.audit.AgentFrontendMappingEvent.AgentFrontendMappingEvent
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object AuditService {

  def auditCheckAgentRefCodeEvent(saAgentReferenceOpt: Option[SaAgentReference], authProviderId: Option[String], authProviderType: Option[String])
                                 (auditService: AuditService)
                                 (implicit hc: HeaderCarrier, request: Request[Any]): Unit = {
    val event = createEvent(AgentFrontendMappingEvent.CheckAgentRefCode,
      Seq("isEnrolledSAAgent" -> saAgentReferenceOpt.isDefined)
        ++ saAgentReferenceOpt.map(r => Seq("saAgentRef" -> r.value)).getOrElse(Seq.empty)
        ++ authProviderId.map(v => Seq("authProviderId" -> v)).getOrElse(Seq.empty)
        ++ authProviderType.map(v => Seq("authProviderType" -> v)).getOrElse(Seq.empty)
    )
    auditService.send(event)
  }

  def toTransactionName(string: String): String = {
    val s = string.replaceAll("([a-z]+(?=[A-Z]))","$1-").replaceAll("([A-Z]+(?![a-z]))","$1-").toLowerCase
    if(s.lastOption.contains('-')) s.init else s
  }

  private def createEvent(event: AgentFrontendMappingEvent,
                          details: Seq[(String, Any)])
                         (implicit hc: HeaderCarrier, request: Request[Any]): DataEvent = {
    DataEvent(
      auditSource = "agent-mapping-frontend",
      auditType = event.toString,
      tags = hc.toAuditTags(toTransactionName(event.toString), request.path),
      detail = hc.toAuditDetails(details.map(pair => pair._1 -> pair._2.toString): _*)
    )
  }

}

trait AuditService {
  def send(event: DataEvent)(implicit hc: HeaderCarrier): Future[Unit]
}

object NoOpAuditService extends AuditService {
  override def send(event: DataEvent)(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(())
}

@Singleton
class AuditServiceImpl @Inject()(val auditConnector: AuditConnector) extends AuditService {

  override def send(event: DataEvent)(implicit hc: HeaderCarrier): Future[Unit] = {
    Future {
      Try(auditConnector.sendEvent(event))
    }
  }
}

object AgentFrontendMappingEvent extends Enumeration {
  type AgentFrontendMappingEvent = Value
  val CheckAgentRefCode = Value
}
