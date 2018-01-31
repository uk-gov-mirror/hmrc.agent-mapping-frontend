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
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.model.Identifier
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{authorisedEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

case class AgentRequest[A](identifiers: Seq[Identifier], request: Request[A]) extends WrappedRequest[A](request)

trait AuthActions extends AuthorisedFunctions with AuthRedirects {

  def env: Environment
  def appConfig: AppConfig

  private val validEnrolments: Seq[(String, String)] = Seq(("IR-SA-AGENT", "IRAgentReference"), ("HMCE-VATDEC-ORG", "VATRegNo"))

  private def toPredicate(enrolments: Seq[(String,String)]): Predicate = {
    enrolments.map(e => Enrolment(e._1)).reduce[Predicate](_ or _)
  }

  def extractIdentifiers(expectedEnrolments: Seq[(String,String)], authorisedEnrolments: Enrolments): Seq[Identifier] = {
    val identifierOpts: Seq[Option[Identifier]] = expectedEnrolments.map {
      case (serviceName, identifierKey) =>
        for {
          enrolment <- authorisedEnrolments.getEnrolment(serviceName)
          identifier <- enrolment.getIdentifier(identifierKey)
        } yield Identifier(identifier.key, identifier.value, enrolment.isActivated)
    }
    identifierOpts.collect {case Some(i) => i}
  }

  private def withAgentEnrolledFor[A](enrolments: Seq[(String,String)])(body: (Seq[Identifier], Credentials) => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    authorised(
      toPredicate(enrolments) and AuthProviders(GovernmentGateway) and Agent)
      .retrieve(authorisedEnrolments and credentials) {
        case justAuthorisedEnrolments ~ creds =>
          val identifiers = extractIdentifiers(enrolments, justAuthorisedEnrolments)
          body(identifiers, creds)
      } recover {
        case _: NoActiveSession => toGGLogin(s"${appConfig.authenticationLoginCallbackUrl}${request.uri}")
    }
  }

  def withAuthorisedAgent(auditService: AuditService = NoOpAuditService)(body: AgentRequest[AnyContent] => Future[Result])(implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
      withAgentEnrolledFor(validEnrolments) {
        case (identifiers, creds) if identifiers.nonEmpty =>
          val (activated, others) = identifiers.partition(_.activated)
          others.foreach { id =>
            AuditService.auditCheckAgentRefCodeEvent(Some(id), Option(creds.providerId), Option(creds.providerType))(auditService)
          }
          if(activated.nonEmpty){
            activated.foreach { id =>
              AuditService.auditCheckAgentRefCodeEvent(Some(id), Option(creds.providerId), Option(creds.providerType))(auditService)
            }
            body(AgentRequest(activated, request))
          } else {
            Future.failed(InsufficientEnrolments("None activated enrolment has been found."))
          }
        case (_, creds) =>
          AuditService.auditCheckAgentRefCodeEvent(None, Option(creds.providerId), Option(creds.providerType))(auditService)
          Future.failed(InsufficientEnrolments("Expected enrolments has not been found"))
      } recover {
        case _: InsufficientEnrolments => Redirect(routes.MappingController.notEnrolled())
      }
  }

}
