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
import uk.gov.hmrc.agentmappingfrontend.model.Identifier
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.Try
import uk.gov.hmrc.http.HeaderCarrier

object AuditService {

  val fieldsByIdentifierKey = Map(
    "IRAgentReference"->"isEnrolledSAAgent",
    "VATRegNo" -> "isEnrolledVATAgent"
  )
  val identifiersByKey = Map(
    "IRAgentReference"->"saAgentRef",
    "VATRegNo" -> "vatAgentRef"
  )

  def toDetailFields(identifier: Identifier): Seq[(String,Any)] = {
    Seq(
      fieldsByIdentifierKey(identifier.key) -> identifier.activated
    ) ++ (
      if(identifier.activated) Seq(identifiersByKey(identifier.key) -> identifier.value) else Seq.empty
      )
  }

  def auditCheckAgentRefCodeEvent(identifier: Option[Identifier], authProviderId: Option[String], authProviderType: Option[String])
                                 (auditService: AuditService)
                                 (implicit hc: HeaderCarrier, request: Request[Any]): Unit = {
    val event = createEvent(AgentFrontendMappingEvent.CheckAgentRefCode, "check-agent-ref-code",
        identifier.map(toDetailFields).getOrElse(Seq("isEnrolledSAAgent"->false,"isEnrolledVATAgent"->false))
        ++ authProviderId.map(v => Seq("authProviderId" -> v)).getOrElse(Seq.empty)
        ++ authProviderType.map(v => Seq("authProviderType" -> v)).getOrElse(Seq.empty)
    )
    auditService.send(event)
  }

  private def createEvent(event: AgentFrontendMappingEvent,
                          transactionName: String,
                          details: Seq[(String, Any)])
                         (implicit hc: HeaderCarrier, request: Request[Any]): DataEvent = {
    DataEvent(
      auditSource = "agent-mapping-frontend",
      auditType = event.toString,
      tags = hc.toAuditTags(transactionName, request.path),
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
