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
import play.api.data._
import play.api.Configuration
import play.api.Environment
import play.api.Logging
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.AgentSubscriptionConnector
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer.No
import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer.Yes
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.repository.ClientCountAndGGTag
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
  clientAuthorisationsAddedTemplate: client_authorisations_added,
  startTemplate: start,
  alreadyMappedTemplate: already_mapped,
  notEnrolledTemplate: not_enrolled,
  incorrectAccountTemplate: incorrect_account,
  mcc: MessagesControllerComponents
)(implicit
  val ec: ExecutionContext,
  val appConfig: AppConfig
)
extends FrontendController(mcc)
with I18nSupport
with AuthActions
with Logging {

  def root: Action[AnyContent] = Action {
    Redirect(routes.MappingController.start)
  }

  private def getBackLinkForStart(implicit request: Request[_]): String = request.session
    .get("OriginForMapping") // set in AIF (agent journey & fastTrack) and the dashboard
    .getOrElse(appConfig.agentServicesFrontendBaseUrl)

  val start: Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(arn) =>
        val clientCountsAndGGTags: Future[Seq[ClientCountAndGGTag]] =
          for {
            mdOpt <- mappingConnector.getMappingDetails(arn)
            details <- mdOpt.fold(Seq.empty[MappingDetails])(md => md.mappingDetails)
          } yield ClientCountAndGGTag(details.count, details.ggTag)

        clientCountsAndGGTags.flatMap { countsAndTags =>
          val activeForm: Form[RadioInputAnswer] =
            if (countsAndTags.isEmpty)
              StartMappingForm.form
            else
              ExistingClientRelationshipsForm.form
          repository
            .create(arn)
            .map(id =>
              Ok(startTemplate(
                id,
                countsAndTags,
                getBackLinkForStart,
                activeForm
              ))
            )
        }

      case None => Future.successful(Redirect(routes.MappingController.needAgentServicesAccount))
    }
  }

  def submitStart(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withCheckForArn {
      case Some(arn) =>
        val clientCountsAndGGTags: Future[Seq[ClientCountAndGGTag]] =
          for {
            mdOpt <- mappingConnector.getMappingDetails(arn)
            details <- mdOpt.fold(Seq.empty[MappingDetails])(md => md.mappingDetails)
          } yield ClientCountAndGGTag(details.count, details.ggTag)

        clientCountsAndGGTags.flatMap { countsAndTags =>
          val activeForm: Form[RadioInputAnswer] =
            if (countsAndTags.isEmpty)
              StartMappingForm.form
            else
              ExistingClientRelationshipsForm.form
          activeForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                BadRequest(
                  startTemplate(
                    id,
                    countsAndTags,
                    getBackLinkForStart,
                    formWithErrors
                  )
                ),
              {
                case Yes => Redirect(routes.SignedOutController.signOutAndRedirect(id))
                case No => Redirect(appConfig.agentServicesFrontendBaseUrl)
              }
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
            case CONFLICT => Future.successful(Redirect(routes.MappingController.alreadyMapped(id)))
            case e => throw new RuntimeException(s"Unexpected response from mapping service: $e")
          }
        case Some(result) if result.agentCode.isDefined =>
          // TODO differentiate between not enrolled and agent code mismatch
          Redirect(routes.MappingController.notEnrolled(id))
        case _ =>
          logger.warn(s"Agent with $id not found in repository or agent is page hopping")
          Redirect(routes.MappingController.start)
      }
    }
  }

  def showClientAuthorisationsAdded(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
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
              saMappings
            ))
          }
        case _ =>
          logger.warn(s"Agent with $id not found in repository or agent is page hopping")
          Redirect(routes.MappingController.start)
      }
    }
  }

  def alreadyMapped(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      Future.successful(Ok(alreadyMappedTemplate(id)))
    }
  }

  def notEnrolled(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      Future.successful(Ok(notEnrolledTemplate(id)))
    }
  }

  def incorrectAccount(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withBasicAuth {
      Future.successful(Ok(incorrectAccountTemplate(id)))
    }
  }

}
