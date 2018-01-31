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

package uk.gov.hmrc.agentmappingfrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Environment}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.agentmappingfrontend.audit.AuditService
import uk.gov.hmrc.agentmappingfrontend.auth.{AgentRequest, AuthActions}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.model.Identifier
import uk.gov.hmrc.agentmappingfrontend.views.html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.{ActionWithMdc, FrontendController}

import scala.concurrent.Future
import scala.concurrent.Future.successful

case class MappingForm(arn: Arn, utr: Utr)

@Singleton
class MappingController @Inject()(override val messagesApi: MessagesApi,
                                  val authConnector: AuthConnector,
                                  mappingConnector: MappingConnector,
                                  auditService: AuditService,
                                  val env: Environment,
                                  val config: Configuration
                                 )(implicit val appConfig: AppConfig)
  extends FrontendController with I18nSupport with AuthActions {

  private val mappingForm = Form(
    mapping(
      "arn" -> mapping(
        "arn" -> arn
      )(Arn.apply)(Arn.unapply),
      "utr" -> mapping(
        "value" -> utr
      )(Utr.apply)(Utr.unapply)
    )(MappingForm.apply)(MappingForm.unapply)
  )

  val root: Action[AnyContent] = ActionWithMdc {
    Redirect(routes.MappingController.start())
  }

  val start: Action[AnyContent] = Action.async { implicit request =>
    Future successful Ok(html.start())
  }

  def startSubmit: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent() {
      implicit request: AgentRequest[AnyContent] =>
        successful(Redirect(routes.MappingController.showAddCode()))
    }
  }

  def showAddCode: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(auditService) {
      implicit request: AgentRequest[AnyContent] =>
        successful(Ok(html.add_code(mappingForm, request.identifiers)))
    }
  }

  def submitAddCode: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent() {
      implicit request: AgentRequest[AnyContent] =>
        mappingForm.bindFromRequest.fold(
          formWithErrors => {
            successful(Ok(html.add_code(formWithErrors, request.identifiers)))
          },
          mappingData => {
            mappingConnector.createMapping(mappingData.utr, mappingData.arn, request.identifiers) map { r: Int =>
              r match {
                case CREATED => Redirect(routes.MappingController.complete())
                case FORBIDDEN => Ok(html.add_code(mappingForm.withGlobalError("These details don't match our records. Check your account number and tax reference."), request.identifiers))
                case CONFLICT => Redirect(routes.MappingController.alreadyMapped())
              }
            }
          }
        )
    }
  }


  val complete: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent() {
      implicit request =>
        successful(Ok(html.complete()))
    }
  }


  val alreadyMapped: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent() {
      implicit request =>
        successful(Ok(html.already_mapped()))
    }
  }

  val notEnrolled: Action[AnyContent] = Action.async { implicit request =>
    Future successful Ok(html.not_enrolled())
  }
}
