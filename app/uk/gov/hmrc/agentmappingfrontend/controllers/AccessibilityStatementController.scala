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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.views.html.accessibility_statement
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

class AccessibilityStatementController @Inject()(
  accessibilityView: accessibility_statement,
  mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig)
    extends FrontendController(mcc) with I18nSupport {

  def showAccessibilityStatement: Action[AnyContent] = Action { implicit request =>
    val userAction: String = request.headers.get(HeaderNames.REFERER).getOrElse("")
    Ok(accessibilityView(appConfig.accessibilityUrl(userAction)))
  }
}
