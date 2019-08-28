/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer.{No, Yes}
import uk.gov.hmrc.agentmappingfrontend.model.{AuthProviderId, ExistingClientRelationshipsForm, GGTagForm, MappingDetailsRequest}
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.repository.{ClientCountAndGGTag, MappingArnRepository, MappingArnResult}
import uk.gov.hmrc.agentmappingfrontend.util._
import uk.gov.hmrc.agentmappingfrontend.views.html
import uk.gov.hmrc.agentmappingfrontend.views.html.client_relationships_found
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MappingController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  mappingConnector: MappingConnector,
  repository: MappingArnRepository,
  val env: Environment,
  val config: Configuration)(implicit val appConfig: AppConfig, val ec: ExecutionContext)
    extends MappingBaseController with I18nSupport with AuthActions {

  val root: Action[AnyContent] = Action {
    Redirect(routes.MappingController.start())
  }

  val start: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(arn) => repository.create(arn).map(id => Ok(html.start(id)))
      case None      => successful(Redirect(routes.MappingController.needAgentServicesAccount()))
    }
  }

  def needAgentServicesAccount: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(_) => successful(Redirect(routes.MappingController.start()))
      case None    => successful(Ok(html.start_sign_in_required()))
    }
  }

  def returnFromGGLogin(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(record) =>
          for {
            clientCount <- mappingConnector.getClientCount
            newRef <- repository.create(
                       record.arn,
                       currentCount = clientCount,
                       clientCountAndGGTags = record.clientCountAndGGTags)
            _ <- repository.delete(id)
          } yield Redirect(routes.MappingController.showClientRelationshipsFound(newRef))

        case None =>
          Logger.warn(s"could not find a record for id $id")
          Redirect(routes.MappingController.start())
      }
    }
  }

  def showClientRelationshipsFound(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(record) =>
          val clientCount = record.currentCount
          //remove this when GG tag pages get put back in
          repository
            .updateClientCountAndGGTag(id, ClientCountAndGGTag(clientCount, ""))
            .map(_ => Ok(client_relationships_found(clientCount, id)))
        case None => Ok(html.page_not_found())
      }
    }
  }

//  def showGGTag(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
//    withAuthorisedAgent(id) { _ =>
//      Ok(html.gg_tag(GGTagForm.form, id))
//    }
//  }
//
//  def submitGGTag(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
//    withAuthorisedAgent(id) { _ =>
//      repository.findRecord(id).flatMap {
//        case Some(record) =>
//          GGTagForm.form.bindFromRequest
//            .fold(
//              formWithErrors => {
//                Ok(html.gg_tag(formWithErrors, id))
//              },
//              ggTag => {
//                for {
//                  _ <- repository.updateCurrentGGTag(id, ggTag.value)
//                  _ <- repository.updateClientCountAndGGTag(id, ClientCountAndGGTag(record.currentCount, ggTag.value))
//                } yield Redirect(routes.MappingController.showExistingClientRelationships(id))
//              }
//            )
//        case None => Ok(html.page_not_found())
//      }
//    }
//  }

  def updateMappingRecordsAndRedirect(
    arn: Arn,
    authProviderId: AuthProviderId,
    record: MappingArnResult,
    id: MappingArnResultId,
    backUrl: String)(implicit request: Request[AnyContent]): Future[Result] =
    for {
      _ <- mappingConnector
            .createOrUpdateMappingDetails(
              arn,
              MappingDetailsRequest(authProviderId, record.currentGGTag, record.currentCount))
      _ <- repository.updateMappingCompleteStatus(id)
    } yield
      Ok(
        html.existing_client_relationships(
          ExistingClientRelationshipsForm.form,
          id,
          record.clientCountAndGGTags,
          taskList = false,
          backUrl))

  def showExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { providerId =>
      repository.findRecord(id).flatMap {
        case Some(record) =>
          val backUrl = routes.MappingController.showClientRelationshipsFound(id).url
          if (!record.alreadyMapped) {
            mappingConnector.createMapping(record.arn).flatMap {
              case CREATED =>
                updateMappingRecordsAndRedirect(record.arn, AuthProviderId(providerId), record, id, backUrl)
              case CONFLICT => Redirect(routes.MappingController.alreadyMapped(id))
              case e =>
                Logger.warn(s"unexpected response from server $e")
                InternalServerError
            }
          } else
            Ok(
              html.existing_client_relationships(
                ExistingClientRelationshipsForm.form,
                id,
                record.clientCountAndGGTags,
                taskList = false,
                backUrl))

        case None => Ok(html.page_not_found())
      }
    }
  }

  def submitExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      ExistingClientRelationshipsForm.form.bindFromRequest
        .fold(
          formWithErrors => {
            repository.findRecord(id).flatMap {
              case Some(record) =>
                Ok(
                  html.existing_client_relationships(
                    formWithErrors,
                    id,
                    record.clientCountAndGGTags,
                    taskList = false,
                    routes.MappingController.showClientRelationshipsFound(id).url))

              case None =>
                Logger.info(s"no record found for id $id")
                Redirect(routes.MappingController.start())
            }
          }, {
            case Yes => Redirect(routes.SignedOutController.signOutAndRedirect(id))
            case No  => Redirect(routes.MappingController.complete(id))
          }
        )
    }
  }

  def complete(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(record) => Ok(html.complete(id, record.clientCountAndGGTags.map(_.clientCount).sum))

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
