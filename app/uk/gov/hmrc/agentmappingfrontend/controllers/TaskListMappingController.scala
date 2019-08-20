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

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentmappingfrontend.auth.TaskListAuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.{AgentSubscriptionConnector, MappingConnector}
import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer.{No, Yes}
import uk.gov.hmrc.agentmappingfrontend.model.{ExistingClientRelationshipsForm, GGTagForm, MappingDetails, UserMapping}
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.repository.TaskListMappingRepository
import uk.gov.hmrc.agentmappingfrontend.services.AgentSubscriptionService
import uk.gov.hmrc.agentmappingfrontend.util._
import uk.gov.hmrc.agentmappingfrontend.views.html.{already_mapped, client_relationships_found, existing_client_relationships, gg_tag, incorrect_account, not_enrolled, start_sign_in_required, start => start_journey}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskListMappingController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentSubscriptionConnector: AgentSubscriptionConnector,
  val agentSubscriptionService: AgentSubscriptionService,
  val mappingConnector: MappingConnector,
  val repository: TaskListMappingRepository,
  val env: Environment,
  val config: Configuration)(implicit val appConfig: AppConfig, val ec: ExecutionContext)
    extends MappingBaseController with I18nSupport with TaskListAuthActions {

  def root(continueId: String): Action[AnyContent] = Action.async { implicit request =>
    Redirect(routes.TaskListMappingController.start(continueId))
  }

  def start(continueId: String): Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth {
      agentSubscriptionConnector.getSubscriptionJourneyRecord(continueId).flatMap {
        case Some(_) =>
          repository
            .create(continueId)
            .flatMap(id => nextPage(id))
        case None =>
          throw new RuntimeException(s"continueId $continueId not recognised")
      }
    }
  }

  def returnFromGGLogin(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(id) { agent =>
      for {
        maybeRecord <- repository.findRecord(id)
        record = maybeRecord.getOrElse(
          throw new RuntimeException(
            s"no task-list mapping record found for agentCode ${agent.agentCodeOpt.getOrElse(" ")}"))
        maybeSjr <- agentSubscriptionConnector.getSubscriptionJourneyRecord(record.continueId)
        sjr = maybeSjr.getOrElse(throw new RuntimeException(
          s"no subscription journey record found after from GG login for agentCode ${agent.agentCodeOpt.getOrElse(" ")}"))
        result <- if (!sjr.userMappings.map(_.authProviderId).contains(agent.authProviderId)) {
                   for {
                     newId <- repository.create(record.continueId)
                     _     <- repository.delete(id)
                     r     <- nextPage(newId)
                   } yield r
                 } else {
                   Future.successful(Ok(already_mapped(id, taskList = true)))
                 }
      } yield result
    }
  }

  def showClientRelationshipsFound(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(id) { agent =>
      repository.findRecord(id).flatMap {
        case Some(record) => {
          if (!record.alreadyMapped) {
            mappingConnector.getClientCount.flatMap(count => {
              repository
                .upsert(record.copy(clientCount = count), record.continueId)
                .map(_ => Ok(client_relationships_found(count, id, taskList = true)))
            })
          } else {
            Ok(client_relationships_found(record.clientCount, id, taskList = true))
          }
        }
        case None => {
          throw new RuntimeException(
            s"no task-list mapping record found for agent code ${agent.agentCodeOpt.getOrElse(" ")}")
        }
      }
    }
  }

  def confirmClientRelationshipsFound(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(id) { agent =>
      repository.findRecord(id).flatMap {
        case Some(record) =>
          if (!record.alreadyMapped) {
            agentSubscriptionConnector.getSubscriptionJourneyRecord(record.continueId).flatMap {
              case Some(sjr) => {
                val newSjr = sjr.copy(
                  userMappings = UserMapping(
                    authProviderId = agent.authProviderId,
                    agentCode = agent.agentCodeOpt,
                    count = record.clientCount,
                    legacyEnrolments = agent.agentEnrolments,
                    ggTag = ""
                  ) :: sjr.userMappings)

                agentSubscriptionService.createOrUpdateRecordOrFail(agent, newSjr, {
                  repository
                    .upsert(record.copy(alreadyMapped = true), record.continueId)
                    .map(_ => Redirect(routes.TaskListMappingController.showGGTag(id)))
                })
              }

              case None =>
                throw new RuntimeException(
                  s"no subscription journey record found in confirmClientRelationshipsFound for agentCode ${agent.agentCodeOpt
                    .getOrElse(" ")}")
            }

          } else {
            Redirect(routes.TaskListMappingController.showExistingClientRelationships(id))
          }
        case None => {
          throw new RuntimeException(
            s"no task-list mapping record found for agent code ${agent.agentCodeOpt.getOrElse(" ")}")
        }
      }
    }

  }

  def showGGTag(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(id) { _ =>
      Ok(gg_tag(GGTagForm.form, id, taskList = true))
    }
  }

  def submitGGTag(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(id) { agent =>
      GGTagForm.form.bindFromRequest
        .fold(
          formWithErrors => {
            Ok(gg_tag(formWithErrors, id))
          },
          ggTag => {
            val updatedUserMappings: List[UserMapping] = agent.getMandatorySubscriptionJourneyRecord.userMappings
              .map(m => if (m.authProviderId == agent.authProviderId) m.copy(ggTag = ggTag.value) else m)
            val newSjr = agent.getMandatorySubscriptionJourneyRecord.copy(userMappings = updatedUserMappings)

            agentSubscriptionService.createOrUpdateRecordOrFail(
              agent,
              newSjr,
              Redirect(continueOrStop(routes.TaskListMappingController.showExistingClientRelationships(id), id)))
          }
        )
    }
  }

  def showExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(id) { agent =>
      backUrl(id).map(
        url =>
          Ok(
            existing_client_relationships(
              ExistingClientRelationshipsForm.form,
              id,
              agent.getMandatorySubscriptionJourneyRecord.userMappings.map(u => u.toClientCountAndGGTag),
              taskList = true,
              url
            ))
      )
    }
  }

  def submitExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent(id) { agent =>
      ExistingClientRelationshipsForm.form.bindFromRequest
        .fold(
          formWithErrors => {
            backUrl(id).flatMap(
              url =>
                Ok(
                  existing_client_relationships(
                    formWithErrors,
                    id,
                    agent.getMandatorySubscriptionJourneyRecord.userMappings.map(u => u.toClientCountAndGGTag),
                    taskList = true,
                    url
                  )))
          }, {
            case Yes => Redirect(continueOrStop(routes.SignedOutController.taskListSignOutAndRedirect(id), id))
            case No  => Redirect(continueOrStop(routes.SignedOutController.returnAfterMapping(), id))
          }
        )
    }
  }

  def incorrectAccount(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth {
      Future.successful(Ok(incorrect_account(id, taskList = true)))
    }
  }

  def notEnrolled(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth {
      Future.successful(Ok(not_enrolled(id, taskList = true)))
    }
  }

  def alreadyMapped(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAgentAuth {
      Future.successful(Ok(already_mapped(id, taskList = true)))
    }
  }

  private def continueOrStop(next: Call, id: MappingArnResultId)(implicit request: Request[AnyContent]): String = {

    val submitAction = request.body.asFormUrlEncoded
      .fold(Seq.empty: Seq[String])(someMap => someMap.getOrElse("continue", Seq.empty))

    val call = submitAction.headOption match {
      case Some("continue") => next.url
      case Some("save") => {
        Logger.info(s"user has selected save and come back later on /existing-client-relationships")
        s"${appConfig.agentSubscriptionFrontendProgressSavedUrl}/task-list/existing-client-relationships/?id=$id"
      }
      case e => {
        throw new RuntimeException(s"unexpected value found in submit $e")
      }
    }
    call
  }

  private def nextPage(
    id: MappingArnResultId)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    withSubscribingAgent(id) { agent =>
      repository.findRecord(id).flatMap {
        case Some(record) => {
          agentSubscriptionConnector.getSubscriptionJourneyRecord(record.continueId).map {
            case Some(sjr) =>
              if (sjr.cleanCredsAuthProviderId.contains(agent.authProviderId)) {
                Logger.info("user entered task list mapping with a clean cred id")
                Ok(start_sign_in_required(Some(id), taskList = true))
              } else if (sjr.userMappings.map(_.authProviderId).isEmpty) {
                Ok(start_journey(id, taskList = true)) //first time here
              } else if (sjr.userMappings.map(_.authProviderId).contains(agent.authProviderId)) {
                Redirect(routes.TaskListMappingController.showExistingClientRelationships(id))
              } else {
                Redirect(routes.TaskListMappingController.showClientRelationshipsFound(id))
              }
            case None =>
              throw new RuntimeException(
                s"no subscription journey record found for agentCode ${agent.agentCodeOpt.getOrElse(" ")} when routing to next page")
          }
        }
        case None =>
          throw new RuntimeException(
            s"no task list mapping record found for id $id and agent code ${agent.agentCodeOpt.getOrElse(" ")}")
      }
    }

  private def backUrl(id: MappingArnResultId): Future[String] =
    repository.findRecord(id).flatMap {
      case Some(record) => {
        if (record.alreadyMapped) {
          routes.TaskListMappingController.showClientRelationshipsFound(id).url
        } else {
          s"${appConfig.agentSubscriptionFrontendExternalUrl}${appConfig.agentSubscriptionFrontendTaskListPath}"
        }
      }
      case None => throw new RuntimeException(s"no task-list mapping record found for id $id for backUrl")
    }

}
