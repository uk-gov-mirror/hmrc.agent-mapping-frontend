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
import uk.gov.hmrc.agentmappingfrontend.util._
import uk.gov.hmrc.agentmappingfrontend.views.html._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.ConflictException
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
  clientRelationShipsFoundTemplate: client_relationships_found,
  pageNotFoundTemplate: page_not_found,
  existingClientRelationshipsTemplate: existing_client_relationships,
  completeTemplate: complete,
  startTemplate: start,
  alreadyMappedTemplate: already_mapped,
  notEnrolledTemplate: not_enrolled,
  incorrectAccountTemplate: incorrect_account,
  ggTagTemplate: gg_tag,
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
    withAuthorisedAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(record) =>
          for {
            clientCount <- mappingConnector.getClientCount
            newRef <- repository
              .create(
                record.arn,
                currentCount = clientCount,
                clientCountAndGGTags = record.clientCountAndGGTags
              )
            _ <- repository.delete(id)
          } yield Redirect(routes.MappingController.showClientRelationshipsFound(newRef))

        case None =>
          logger.warn(s"could not find a record for id $id")
          Redirect(routes.MappingController.start)
      }
    }
  }

  def showClientRelationshipsFound(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(record) => Ok(clientRelationShipsFoundTemplate(record.currentCount, id))

        case None => Ok(pageNotFoundTemplate())
      }
    }
  }

  def showGGTag(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      Ok(ggTagTemplate(GGTagForm.form, id))
    }
  }

  def submitGGTag(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { providerId =>
      repository.findRecord(id).flatMap {
        case Some(record) =>
          GGTagForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => Ok(ggTagTemplate(formWithErrors, id)),
              ggTag =>
                if (!record.alreadyMapped) {
                  mappingConnector.createMapping(record.arn).flatMap {
                    case CREATED =>
                      val clientCountAndGGTags = ClientCountAndGGTag(record.currentCount, ggTag.value) +: record.clientCountAndGGTags
                      val newRecord = record.copy(clientCountAndGGTags = clientCountAndGGTags, currentGGTag = ggTag.value)
                      for {
                        _ <- mappingConnector
                          .createOrUpdateMappingDetails(
                            record.arn,
                            MappingDetailsRequest(
                              AuthProviderId(providerId),
                              ggTag.value,
                              record.currentCount
                            )
                          )
                        _ <- repository.updateMappingCompleteStatus(id)
                        _ <- repository.upsert(newRecord, id)
                      } yield Redirect(routes.MappingController.showExistingClientRelationships(id))
                    case CONFLICT => Redirect(routes.MappingController.alreadyMapped(id))
                    case e =>
                      logger.warn(s"unexpected response from server $e")
                      InternalServerError
                  }
                }
                else
                  Redirect(routes.MappingController.showExistingClientRelationships(id))
            )
        case None => Ok(pageNotFoundTemplate())
      }
    }.recover { case _: ConflictException => Redirect(routes.MappingController.alreadyMapped(id)) }
  }

  def showExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(record) =>
          Ok(
            existingClientRelationshipsTemplate(
              ExistingClientRelationshipsForm.form,
              id,
              record.clientCountAndGGTags,
              routes.MappingController.showGGTag(id).url,
              taskList = false
            )
          )

        case None => Ok(pageNotFoundTemplate())
      }
    }
  }

  def submitExistingClientRelationships(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      ExistingClientRelationshipsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            repository.findRecord(id).flatMap {
              case Some(record) =>
                Ok(
                  existingClientRelationshipsTemplate(
                    formWithErrors,
                    id,
                    record.clientCountAndGGTags,
                    routes.MappingController.showClientRelationshipsFound(id).url,
                    taskList = false
                  )
                )

              case None =>
                logger.info(s"no record found for id $id")
                Redirect(routes.MappingController.start)
            },
          {
            case Yes => Redirect(routes.SignedOutController.signOutAndRedirect(id))
            case No => Redirect(routes.MappingController.complete(id))
          }
        )
    }
  }

  def complete(id: MappingArnResultId): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAgent(id) { _ =>
      repository.findRecord(id).flatMap {
        case Some(record) => Ok(completeTemplate(id, record.clientCountAndGGTags.map(_.clientCount).sum))

        case None =>
          logger.warn("user must not completed the mapping journey or have lost the stored arn")
          InternalServerError
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
