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

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.repository.MappingArnResult.MappingArnResultId
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.helper.urlEncode

import scala.concurrent.Future

class SignedOutController @Inject()(appConfig: AppConfig) extends FrontendController {

  def signOutAndRedirect(id: MappingArnResultId): Action[AnyContent] = Action { implicit request =>
    val url = s"${appConfig.signOutRedirectUrl}?id=$id"
    val signOutAndRedirectUrl: String =
      s"${appConfig.companyAuthFrontendExternalUrl}${appConfig.ggSignIn}?continue=${urlEncode(url)}"

    Redirect(signOutAndRedirectUrl)
  }

  def reLogForMappingStart: Action[AnyContent] = Action { implicit request =>
    Redirect(appConfig.signInAndContinue).withNewSession
  }
}
