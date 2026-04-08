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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.MessagesRequest
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.views.html.timed_out
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.Future

class SignedOutController @Inject() (
  timedOutTemplate: timed_out,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig
)
extends FrontendController(cc):

  private def signOutWithContinue(continue: String) =
    val signOutAndRedirectUrl: String = uri"${appConfig.signOutUrl}?${Map("continue" -> continue)}".toString
    Redirect(signOutAndRedirectUrl)

  def signOutAndRedirect(id: MappingArnResultId): Action[AnyContent] = Action:
    val url = uri"${appConfig.signOutRedirectUrl}?${Map("id" -> id)}"
    signOutWithContinue(url.toString)

  def reLogForMappingStart: Action[AnyContent] = Action:
    signOutWithContinue(appConfig.signInAndContinue)

  def signOut: Action[AnyContent] = Action:
    val url = uri"${appConfig.agentMappingFrontendBaseUrl + routes.MappingController.root.url}"
    signOutWithContinue(url.toString)

  def timeOut(): Action[AnyContent] = Action:
    val url = uri"${appConfig.agentMappingFrontendBaseUrl + routes.SignedOutController.timedOut.url}"
    signOutWithContinue(url.toString)

  def timedOut: Action[AnyContent] = Action.async: request =>
    given MessagesRequest[?] = request
    Future successful Forbidden(timedOutTemplate())
