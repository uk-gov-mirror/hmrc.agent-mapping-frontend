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

package uk.gov.hmrc.agentmappingfrontend.auth

import play.api.mvc.Results._
import play.api.mvc._
import play.api.Configuration
import play.api.Environment
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentmappingfrontend.auth.EnrolmentHelper._
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait AuthActions
extends AuthorisedFunctions
with RequestAwareLogging {

  def env: Environment

  def appConfig: AppConfig

  def config: Configuration

  def withBasicAuth(
    body: => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway)) {
      body
    } recover {
      handleException
    }
  }

  def withAuthorisedSaAgent(idRefToArn: MappingArnResultId)(
    body: Enrolment => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(allEnrolments and credentials and affinityGroup) {
        case _ ~ None ~ _ => Future.successful(Forbidden)
        case agentEnrolments ~ Some(_) ~ Some(affinityGroup) =>
          val activeEnrolments = agentEnrolments.enrolments.filter(_.isActivated)
          val saEnrolment = activeEnrolments.find(_.key == IRAgentReference.serviceKey)

          saEnrolment match {
            case Some(enrolment) => body(enrolment)
            case _ if userHasAsAgentEnrolment(activeEnrolments) => Future.successful(Redirect(routes.MappingController.wrongSignInDetailsAsa(idRefToArn)))
            case _ if affinityGroup != AffinityGroup.Agent => Future.successful(Redirect(routes.MappingController.wrongSignInDetailsNotAgent(idRefToArn)))
            case _ => Future.successful(Redirect(routes.MappingController.problemWithDetails(idRefToArn)))
          }
      }
      .recover {
        handleException
      }
  }

  def withCheckForArn(
    body: Option[Arn] => Future[Result]
  )(implicit
    request: Request[_],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(allEnrolments) { agentEnrolments =>
        val arn = getArn(agentEnrolments)
        body(arn)
      }
      .recoverWith {
        case _: NoActiveSession => body(None)

        case e => Future.successful(handleException.apply(e))
      }
  }

  def withBasicAgentAuth[A](
    body: => Future[Result]
  )(implicit
    request: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent) {
      body
    } recover {
      handleException
    }
  }

  private def getArn(enrolments: Enrolments) =
    for {
      enrolment <- enrolments.getEnrolment(AsAgentServiceKey)
      identifier <- enrolment.getIdentifier(ArnEnrolmentKey)
    } yield Arn(identifier.value)

  private def handleException(implicit request: Request[_]): PartialFunction[Throwable, Result] = {

    case _: UnsupportedAffinityGroup =>
      logger.warn(s"Logged in user does not have the required affinity group")
      Forbidden

    case _: NoActiveSession =>
      val continueUrl = uri"${continueBaseUrl + request.uri}"
      val params = List(
        "continue_url" -> continueUrl,
        "origin" -> appName
      )
      val url = uri"""${basGatewayFrontendExternalUrl + signInUrl}?${params}"""
      Redirect(url.toString)

    case _: InsufficientEnrolments =>
      logger.warn(s"Logged in user does not have required enrolments")
      Forbidden

    case _: UnsupportedAuthProvider =>
      logger.warn("User is not logged in via  GovernmentGateway, signing out and redirecting")
      val continueUrl = uri"${continueBaseUrl + request.uri}"
      val params = List(
        "continue_url" -> continueUrl
      )
      val url = uri"""${basGatewayFrontendExternalUrl + signInUrl}?${params}"""
      Redirect(url.toString)
  }

  private val basGatewayFrontendExternalUrl = getString("microservice.services.bas-gateway-frontend.external-url")
  private val signInUrl = getString("microservice.services.bas-gateway-frontend.sign-in.path")
  private val continueBaseUrl = getString("microservice.services.agent-mapping-frontend.external-url")
  private val appName = getString("appName")

  private def getString(key: String): String = config.underlying.getString(key)

}
