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

package uk.gov.hmrc.agentmappingfrontend.auth

import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Environment, Mode}
import uk.gov.hmrc.agentmappingfrontend.audit.{AuditService, NoOpAuditService}
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.authorisedEnrolments
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

case class AgentRequest[A](saAgentReference: SaAgentReference, request: Request[A]) extends WrappedRequest[A](request)

trait AuthActions extends AuthorisedFunctions with AuthRedirects {

  def env: Environment

  private def withAgentEnrolledFor[A](serviceName: String, identifierKey: String)(body: Option[String] => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    authorised(
      Enrolment(serviceName) and AuthProviders(GovernmentGateway) and Agent)
      .retrieve(authorisedEnrolments) { enrolments =>
        val id = for {
          enrolment <- enrolments.getEnrolment(serviceName)
          identifier <- enrolment.getIdentifier(identifierKey)
        } yield identifier.value
        body(id)
      } recover {
        case _: NoActiveSession => toGGLogin(if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}")
    }
  }

  def withAuthorisedSAAgent(auditService: AuditService = NoOpAuditService)(body: AgentRequest[AnyContent] => Future[Result])(implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
      withAgentEnrolledFor("IR-SA-AGENT", "IRAgentReference") {
        case Some(iRAgentReference) =>
          val saAgentReference = SaAgentReference(iRAgentReference)
          AuditService.auditCheckAgentRefCodeEvent(Some(saAgentReference), None, None)(auditService)
          body(AgentRequest(saAgentReference,request))
        case None =>
          AuditService.auditCheckAgentRefCodeEvent(None, None, None)(auditService)
          Future.failed(InsufficientEnrolments("IRAgentReference identifier not found"))
      } recover {
        case _: InsufficientEnrolments => Redirect(routes.MappingController.notEnrolled())
      }
  }

}
