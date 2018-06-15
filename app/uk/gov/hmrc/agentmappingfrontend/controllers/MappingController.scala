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

import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentmappingfrontend.audit.AuditService
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.views.html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.auth.core.{AuthConnector, EnrolmentIdentifier}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.{ActionWithMdc, FrontendController}

import scala.concurrent.Future
import scala.concurrent.Future.successful

case class MappingFormArn(arn: Arn)
case class MappingFormUtr(utr: Utr)

@Singleton
class MappingController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  mappingConnector: MappingConnector,
  auditService: AuditService,
  val env: Environment,
  val config: Configuration)(implicit val appConfig: AppConfig)
    extends FrontendController with I18nSupport with AuthActions {

  import MappingController.mappingFormArn
  import MappingController.mappingFormUtr

  val root: Action[AnyContent] = ActionWithMdc {
    Redirect(routes.MappingController.start())
  }

  val start: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn { enrolmentIdentifier: Option[EnrolmentIdentifier] =>
      Future successful Ok(html.start(enrolmentIdentifier.map(identifier => prettify(Arn(identifier.value)))))
    }
  }

  def startSubmit: Action[AnyContent] = Action.async { implicit request =>
    successful(Redirect(routes.MappingController.showEnterAccountNo).withNewSession)
  }

  def showEnterAccountNo: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgentAudited { providerId =>
      successful(Ok(html.enter_account_number(mappingFormArn)))
    }(AuditService.auditCheckAgentRefCodeEvent(auditService))
  }

  def submitEnterAccountNo: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent { providerId =>
      mappingFormArn.bindFromRequest.fold(
        formWithErrors => {
          successful(Ok(html.enter_account_number(formWithErrors)))
        },
        formArn => {
          successful(Redirect(routes.MappingController.showEnterUtr).addingToSession(("mappingArn", formArn.arn.value)))
        }
      )
    }
  }

  def showEnterUtr: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent { providerId =>
      request.session.get("mappingArn").isDefined match {
        case true  => successful(Ok(html.enter_utr(mappingFormUtr)))
        case false => successful(Redirect(routes.MappingController.showEnterAccountNo()))
      }
    }
  }

  def submitEnterUtr: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent { providerId: String =>
      mappingFormUtr.bindFromRequest.fold(
        formWithErrors => {
          successful(Ok(html.enter_utr(formWithErrors)))
        },
        formWithUtr => {
          request.session.get("mappingArn") match {
            case Some(arn) =>
              mappingConnector.createMapping(formWithUtr.utr, Arn(arn)) map {
                case CREATED   => Redirect(routes.MappingController.complete())
                case FORBIDDEN => Redirect(routes.MappingController.noMatch())
                case CONFLICT  => Redirect(routes.MappingController.alreadyMapped())
              }
            case None => successful(Redirect(routes.MappingController.showEnterAccountNo()))
          }
        }
      )
    }
  }

  def complete(): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent { providerId =>
      val arn = request.session
        .get("mappingArn")
        .getOrElse(
          throw new InternalServerException("user must not completed the mapping journey or have lost the stored arn"))
      successful(Ok(html.complete(providerId, prettify(Arn(arn)))))
    }
  }

  val alreadyMapped: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      successful(Ok(html.already_mapped()))
    }
  }

  val notEnrolled: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      successful(Ok(html.not_enrolled()))
    }
  }

  val noMatch: Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      successful(Ok(html.no_match()))
    }
  }
}

object MappingController {
  val mappingFormArn = Form(
    mapping(
      "arn" -> mapping(
        "arn" -> arn
      )(normalizeArn(_).getOrElse(throw new Exception("Invalid Arn after validation")))(arn => Some(arn.value))
    )(MappingFormArn.apply)(MappingFormArn.unapply)
  )

  val mappingFormUtr = Form(
    mapping(
      "utr" -> mapping(
        "value" -> utr
      )(normalizeUtr(_).getOrElse(throw new Exception("Invalid Utr after validation")))(utr => Some(utr.value))
    )(MappingFormUtr.apply)(MappingFormUtr.unapply)
  )
}
