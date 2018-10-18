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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnRepository
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.{ActionWithMdc, FrontendController}

import scala.concurrent.Future.successful

@Singleton
class MappingController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  mappingConnector: MappingConnector,
  repository: MappingArnRepository,
  val env: Environment,
  val config: Configuration)(implicit val appConfig: AppConfig)
    extends FrontendController with I18nSupport with AuthActions {

  val root: Action[AnyContent] = ActionWithMdc {
    Redirect(routes.MappingController.start())
  }

  val start: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(arn) => repository.create(arn).map(id => Ok(html.start(id)))
      case None      => successful(Redirect(routes.MappingController.needAgentServicesAccount))
    }
  }

  def needAgentServicesAccount: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(_) => successful(Redirect(routes.MappingController.start()))
      case None    => successful(Ok(html.start_sign_in_required()))
    }
  }

  def startSubmit(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findArn(id).flatMap {
        case Some(arn) => {
          for {
            newRefForArn <- repository.create(arn)
            doMappingResult <- mappingConnector.createMapping(arn).map {
                                case CREATED =>
                                  Redirect(routes.MappingController.complete(newRefForArn))
                                case CONFLICT => Redirect(routes.MappingController.alreadyMapped(newRefForArn))
                              }
            _ <- repository.delete(id)
          } yield doMappingResult
        }
        case None =>
          successful(Ok(html.page_not_found()))
      }
    }
  }

  def complete(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findArn(id).map {
        case Some(_) => Ok(html.complete(id))
        case None =>
          throw new InternalServerException("user must not completed the mapping journey or have lost the stored arn")
      }
    }
  }

  def alreadyMapped(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      successful(Ok(html.already_mapped(id)))
    }
  }

  def notEnrolled(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      successful(Ok(html.not_enrolled(id)))
    }
  }

  def incorrectAccount(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      successful(Ok(html.incorrect_account(id)))
    }
  }
}
