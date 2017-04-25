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

package uk.gov.hmrc.agentmappingfrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.views.html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future.successful

case class MappingForm(arn: Arn, utr: Utr)

@Singleton
class MappingController @Inject()(override val messagesApi: MessagesApi,
                                  override val authConnector: AuthConnector,
                                  mappingConnector: MappingConnector)  (implicit appConfig: AppConfig)
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

  val root: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.MappingController.start())
  }

  val start: Action[AnyContent] = Action { implicit request =>
    Ok(html.start())
  }

  val showAddCode: Action[AnyContent] = AuthorisedSAAgent { implicit authContext =>implicit request =>
    successful(Ok(html.add_code(mappingForm, request.saAgentReference)))
  }

  val submitAddCode: Action[AnyContent] = AuthorisedSAAgent { implicit authContext =>implicit request =>
    mappingForm.bindFromRequest.fold(
      formWithErrors => {
        successful(Ok(html.add_code(formWithErrors, request.saAgentReference)))
      },
      mappingData => {
        mappingConnector.createMapping(mappingData.utr, mappingData.arn, request.saAgentReference) map { r : Int =>
          r match {
            case CREATED => Redirect(routes.MappingController.complete(mappingData.arn, request.saAgentReference))
            case FORBIDDEN => Ok(html.add_code(mappingForm.withGlobalError("Those details do not match the details we have for your business"), request.saAgentReference))
            case CONFLICT => Redirect(routes.MappingController.alreadyMapped(mappingData.arn, request.saAgentReference))
          }
        }
      }
    )
  }

  def complete(arn: Arn, saAgentReference: SaAgentReference) : Action[AnyContent] = AuthorisedSAAgent { implicit authContext => implicit request =>
    successful(Ok(html.complete(arn,saAgentReference)))
  }

  def alreadyMapped(arn: Arn, saAgentReference: SaAgentReference) : Action[AnyContent] = AuthorisedSAAgent { implicit authContext => implicit request =>
    successful(Ok(html.already_mapped(arn,saAgentReference)))
  }

  val notEnrolled: Action[AnyContent] = Action { implicit request =>
    Ok(html.not_enrolled())
  }
}
