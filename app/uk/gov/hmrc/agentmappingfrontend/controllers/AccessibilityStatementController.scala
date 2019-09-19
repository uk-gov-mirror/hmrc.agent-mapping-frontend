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

import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.views.html
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext

class AccessibilityStatementController @Inject()()(
  implicit val appConfig: AppConfig,
  metrics: Metrics,
  implicit val messagesApi: MessagesApi)
    extends MappingBaseController() with I18nSupport {

  def showAccessibilityStatement: Action[AnyContent] = Action { implicit request =>
    Ok(html.accessibility_statement())
  }

}
