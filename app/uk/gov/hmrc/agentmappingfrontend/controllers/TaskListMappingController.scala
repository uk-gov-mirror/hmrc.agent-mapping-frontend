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
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentmappingfrontend.auth.TaskListAuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.{AgentSubscriptionConnector, MappingConnector}
import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer.{No, Yes}
import uk.gov.hmrc.agentmappingfrontend.model.{ExistingClientRelationshipsForm, UserMapping}
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.repository.TaskListMappingRepository
import uk.gov.hmrc.agentmappingfrontend.util._
import uk.gov.hmrc.agentmappingfrontend.views.html.{client_relationships_found, existing_client_relationships}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskListMappingController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val agentSubscriptionConnector: AgentSubscriptionConnector,
  val mappingConnector: MappingConnector,
  val repository: TaskListMappingRepository,
  val env: Environment,
  val config: Configuration)(implicit val appConfig: AppConfig, val ec: ExecutionContext)
    extends MappingBaseController with I18nSupport with TaskListAuthActions {

  def start: Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { agent =>
      {
        val continueId: String = agent.getMandatorySubscriptionJourneyRecord.continueId
          .getOrElse(throw new RuntimeException("continueId not found in agent subscription record"))
        repository
          .create(continueId)
          .flatMap(id => nextPage(id))
      }
    }
  }

  def returnFromGGLogin(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    for {
      maybeRecord <- repository.findRecord(id)
      record = maybeRecord.getOrElse(throw new RuntimeException("no task-list mapping record"))
      newId  <- repository.create(record.continueId)
      _      <- repository.delete(id)
      result <- nextPage(newId)
    } yield result
  }

  def showClientRelationshipsFound(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    repository.findRecord(id).flatMap {
      case Some(record) =>
        mappingConnector.getClientCount.flatMap(count => {
          repository
            .upsert(record.copy(clientCount = count), record.continueId)
            .map(_ => Ok(client_relationships_found(count, id, true)))
        })
      case None => Future.successful(Ok("hmmm"))
    }
  }

  def confirmClientRelationshipsFound(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { agent =>
      for {
        maybeRecord <- repository.findRecord(id)
        record = maybeRecord.getOrElse(throw new RuntimeException("no task-list mapping record found!"))
        maybesjr <- agentSubscriptionConnector.getSubscriptionJourneyRecord(record.continueId)
        sjr = maybesjr.getOrElse(throw new RuntimeException("no SJR found"))
        newSjr = sjr.copy(
          userMappings = UserMapping(
            authProviderId = agent.authProviderId,
            agentCodes = Seq(AgentCode(agent.agentCode)),
            count = record.clientCount,
            ggTag = "") :: sjr.userMappings)
        _      <- agentSubscriptionConnector.createOrUpdateJourney(newSjr)
        result <- Redirect(routes.TaskListMappingController.showExistingClientRelationships(id))

      } yield result
    }
  }

  def showExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { agent =>
      Ok(
        existing_client_relationships(
          ExistingClientRelationshipsForm.form,
          id,
          agent.getMandatorySubscriptionJourneyRecord.userMappings.map(_.count),
          true))
    }
  }

  def submitExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withSubscribingAgent { agent =>
      ExistingClientRelationshipsForm.form.bindFromRequest
        .fold(
          formWithErrors => {
            Ok(
              existing_client_relationships(
                formWithErrors,
                id,
                agent.getMandatorySubscriptionJourneyRecord.userMappings.map(_.count)))
          }, {
            case Yes => Redirect(routes.SignedOutController.taskListSignOutAndRedirect(id))
            case No => {
              //agentSubscriptionConnector.getSubscriptionJourneyRecord(agent.getMandatorySubscriptionJourneyRecord)
              Ok("finished -- remember to update record with mapping complete flag")
            }
          }
        )
    }
  }

  private def nextPage(
    id: MappingArnResultId)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    withSubscribingAgent { agent =>
      repository.findRecord(id).flatMap {
        case Some(record) => {
          agentSubscriptionConnector.getSubscriptionJourneyRecord(record.continueId).map {
            case Some(sjr) =>
              if (sjr.userMappings.map(_.authProviderId).contains(agent.authProviderId)) {
                Redirect(routes.TaskListMappingController.showExistingClientRelationships(id))
              } else {
                Redirect(routes.TaskListMappingController.showClientRelationshipsFound(id))
              }
            case None => ???
          }

        }
        case None => Future.successful(Redirect(routes.TaskListMappingController.showClientRelationshipsFound(id)))
      }
    }
}
