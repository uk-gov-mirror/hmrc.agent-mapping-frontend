/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.views.html.timed_out
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.helper.urlEncode

import scala.concurrent.Future

class SignedOutController @Inject()(timedOutTemplate: timed_out, cc: MessagesControllerComponents)(
  implicit appConfig: AppConfig)
    extends FrontendController(cc) {

  def signOutAndRedirect(id: MappingArnResultId): Action[AnyContent] = Action {
    val url = s"${appConfig.signOutRedirectUrl}?id=$id"
    val signOutAndRedirectUrl: String =
      s"${appConfig.companyAuthFrontendBaseUrl}/gg/sign-in?continue=${urlEncode(url)}"

    Redirect(signOutAndRedirectUrl)
  }

  def taskListSignOutAndRedirect(id: MappingArnResultId): Action[AnyContent] = Action {
    val url = s"${appConfig.taskListSignOutRedirectUrl}?id=$id"
    Redirect(constructRedirectUrl(url))
  }

  private def constructRedirectUrl(continue: String): String =
    s"${appConfig.companyAuthFrontendBaseUrl}/gg/sign-in?continue=${urlEncode(continue)}"

  def reLogForMappingStart: Action[AnyContent] = Action {
    Redirect(appConfig.signInAndContinue).withNewSession
  }

  def taskList(): Action[AnyContent] = Action.async {
    val url = appConfig.agentSubscriptionFrontendTaskListUrl
    Future.successful(Redirect(url))
  }

  def returnAfterMapping(): Action[AnyContent] = Action.async {
    val url =
      s"${appConfig.agentSubscriptionFrontendBaseUrl}/return-after-mapping"
    Future.successful(Redirect(url))
  }

  def signOut: Action[AnyContent] = Action {
    startNewSession
  }

  private def startNewSession: Result =
    Redirect(routes.MappingController.root()).withNewSession

  def keepAlive: Action[AnyContent] = Action.async {
    Future successful Ok("OK")
  }

  def timedOut: Action[AnyContent] = Action.async { implicit request =>
    Future successful Forbidden(timedOutTemplate())
  }
}
