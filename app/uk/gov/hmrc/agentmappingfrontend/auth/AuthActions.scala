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

package uk.gov.hmrc.agentmappingfrontend.auth

import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrier.fromHeadersAndSession
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.domain.SaAgentReference

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class AgentRequest[A](saAgentReference: SaAgentReference, request: Request[A]) extends WrappedRequest[A](request)

trait AuthActions extends Actions {
  protected type AsyncPlayUserRequest = AuthContext => AgentRequest[AnyContent] => Future[Result]
  protected type PlayUserRequest = AuthContext => AgentRequest[AnyContent] => Result
  private implicit def hc(implicit request: Request[_]): HeaderCarrier = fromHeadersAndSession(request.headers, Some(request.session))

  private def saAgentReference(e: List[Enrolment]): Option[SaAgentReference] = {
    e.find(e => e.key == "IR-SA-AGENT" && e.state == "Activated") flatMap { e =>
      e.identifiers.find(_.key == "IrAgentReference")
        .map(i => SaAgentReference(i.value))
    }
  }

  def AuthorisedSAAgent(body: AsyncPlayUserRequest): Action[AnyContent] =
    AuthorisedFor(NoOpRegime, pageVisibility = GGConfidence).async {
      implicit authContext => implicit request =>
        isAgentAffinityGroup() flatMap {
          case true => enrolments flatMap {
              e => {
                saAgentReference(e) map { saEnrolment =>
                  body(authContext)(AgentRequest(saEnrolment, request))
                } getOrElse {
                  Future successful redirectToNotEnrolled
                }
              }
            }
          case false => Future successful redirectToNotEnrolled
        }
    }

  private def enrolments(implicit authContext: AuthContext, hc: HeaderCarrier): Future[List[Enrolment]] =
    authConnector.getEnrolments[List[Enrolment]](authContext)

  private def isAgentAffinityGroup()(implicit authContext: AuthContext, hc: HeaderCarrier): Future[Boolean] =
    authConnector.getUserDetails(authContext).map { userDetailsResponse =>
      val affinityGroup = (userDetailsResponse.json \ "affinityGroup").as[String]
      affinityGroup == "Agent"
    }

  private def redirectToNotEnrolled =
    Redirect(routes.MappingController.notEnrolled())
}
