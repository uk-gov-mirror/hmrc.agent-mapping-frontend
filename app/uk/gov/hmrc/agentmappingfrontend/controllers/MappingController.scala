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

package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.Configuration
import play.api.Environment
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnRepository
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnResult
import uk.gov.hmrc.agentmappingfrontend.util._
import uk.gov.hmrc.agentmappingfrontend.views.html._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class MappingController @Inject() (
  val authConnector: AuthConnector,
  mappingConnector: MappingConnector,
  val agentSubscriptionConnector: AgentSubscriptionConnector,
  repository: MappingArnRepository,
  val config: Configuration,
  val env: Environment,
  signInTemplate: start_sign_in_required,
  agentCodeTemplate: agent_code,
  useTheGgUserIdTemplate: use_the_gg_user_id,
  clientAuthorisationsAddedTemplate: client_authorisations_added,
  wrongSignInDetailsTemplate: wrong_sign_in_details,
  problemWithDetailsTemplate: problem_with_details,
  startTemplate: start,
  mcc: MessagesControllerComponents
)(implicit
  val ec: ExecutionContext,
  val appConfig: AppConfig
)
extends FrontendController(mcc)
with I18nSupport
with AuthActions {

  def root: Action[AnyContent] = Action {
    Redirect(routes.MappingController.start)
  }

  private def getBackLinkForStart(implicit request: Request[_]): String = request.session
    .get("OriginForMapping") // set in AIF (agent journey & fastTrack) and the dashboard
    .getOrElse(appConfig.agentServicesFrontendBaseUrl)

  val start: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(arn) =>
        mappingConnector.findSaMappingsFor(arn).flatMap { agentCodes =>
          repository
            .create(arn)
            .map(id =>
              Ok(startTemplate(
                id,
                agentCodes,
                getBackLinkForStart
              ))
            )
        }

      case None => Future.successful(Redirect(routes.MappingController.needAgentServicesAccount))
    }
  }

  def needAgentServicesAccount: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(_) => Future.successful(Redirect(routes.MappingController.start))
      case None => Future.successful(Ok(signInTemplate()))
    }
  }

  def showAgentCode(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      repository.findRecord(id).map {
        case Some(record) =>
          val form =
            record.agentCode match {
              case Some(code) => AgentCodeForm.form.fill(code)
              case None => AgentCodeForm.form
            }
          Ok(agentCodeTemplate(
            form,
            record.mappedAgentCode.isDefined,
            id
          ))
        case _ =>
          logger.warn(s"Agent with $id not found in repository or agent is page hopping")
          Redirect(routes.MappingController.start)
      }
    }
  }

  def submitAgentCode(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      repository.findRecord(id).flatMap {
        case Some(record) =>
          AgentCodeForm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                BadRequest(agentCodeTemplate(
                  formWithErrors,
                  record.mappedAgentCode.isDefined,
                  id
                )),
              agentCode =>
                mappingConnector.findSaMappingsFor(record.arn).flatMap { saMappings =>
                  if (saMappings.map(_.saAgentReference).contains(agentCode)) {
                    Future.successful(BadRequest(agentCodeTemplate(
                      AgentCodeForm.form.withError(
                        AgentCodeForm.fieldName,
                        "agentCode.error.alreadyMapped"
                      ).fill(agentCode),
                      record.mappedAgentCode.isDefined,
                      id
                    )))
                  }
                  else {
                    repository.replace(record.copy(agentCode = Some(agentCode)), id).map { _ =>
                      Redirect(routes.MappingController.showUseTheGgUserId(id))
                    }
                  }
                }
            )
        case _ =>
          logger.warn(s"Agent with $id not found in repository or agent is page hopping")
          Future.successful(Redirect(routes.MappingController.start))
      }
    }
  }

  def showUseTheGgUserId(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      repository.findRecord(id).map {
        case Some(MappingArnResult(
              _,
              _,
              Some(agentCode),
              _,
              _,
              _
            )) =>
          Ok(useTheGgUserIdTemplate(agentCode, id))
        case _ =>
          logger.warn(s"Agent with $id not found in repository or agent is page hopping")
          Redirect(routes.MappingController.start)
      }
    }
  }

  def returnFromGGLogin(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedSaAgent(id) { enrolment =>
      repository.findRecord(id).flatMap {
        case Some(record @ MappingArnResult(
              _,
              arn,
              Some(agentCode),
              _,
              _,
              _
            )) if enrolment.identifiers.map(_.value).contains(agentCode) =>
          mappingConnector.createMapping(arn).flatMap {
            case CREATED =>
              for {
                clientCount <- mappingConnector.getClientCount
                newRecord = record.copy(
                  arn = arn,
                  agentCode = None,
                  mappedAgentCode = Some(agentCode),
                  mappedClientCount = Some(clientCount)
                )
                _ <- repository.replace(newRecord, id)
              } yield Redirect(routes.MappingController.showClientAuthorisationsAdded(newRecord.id))
            case CONFLICT => throw new RuntimeException("Agent is already mapped - unexpected state as this was checked earlier")
            case e => throw new RuntimeException(s"Unexpected response from mapping service: $e")
          }
        case Some(result) if result.agentCode.isDefined => Redirect(routes.MappingController.problemWithDetails(id))
        case _ =>
          logger.warn(s"Agent with $id not found in repository or agent is page hopping")
          Redirect(routes.MappingController.start)
      }
    }
  }

  def showClientAuthorisationsAdded(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedSaAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(MappingArnResult(
              _,
              arn,
              _,
              Some(mappedAgentCode),
              Some(mappedClientCount),
              _
            )) =>
          mappingConnector.findSaMappingsFor(arn).map { saMappings =>
            Ok(clientAuthorisationsAddedTemplate(
              mappedAgentCode,
              mappedClientCount,
              saMappings,
              id
            ))
          }
        case _ =>
          logger.warn(s"Agent with $id not found in repository or agent is page hopping")
          Redirect(routes.MappingController.start)
      }
    }
  }

  def wrongSignInDetailsAsa(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      Future.successful(Ok(wrongSignInDetailsTemplate(id, isAsa = true)))
    }
  }

  def wrongSignInDetailsNotAgent(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      Future.successful(Ok(wrongSignInDetailsTemplate(id, isAsa = false)))
    }
  }

  def problemWithDetails(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      Future.successful(Ok(problemWithDetailsTemplate(id)))
    }
  }

}
