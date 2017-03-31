/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentmappingfrontend.auth.{AuthActions, Enrolment}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.views.html
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future.successful

@Singleton
class MappingController @Inject()(override val messagesApi: MessagesApi, override val authConnector: AuthConnector)  (implicit appConfig: AppConfig)
  extends FrontendController with I18nSupport with AuthActions {
  val start: Action[AnyContent] = Action { implicit request =>
    Ok(html.start_template())
  }

  val addCode: Action[AnyContent] = AuthorisedSAAgent { implicit authContext =>implicit request =>
    successful(Ok(html.add_code_template(saReference(request.enrolments))))
  }

  val complete: Action[AnyContent] = AuthorisedSAAgent { implicit authContext =>implicit request =>
    successful(Ok(html.complete_template()))
  }

  val notEnrolled: Action[AnyContent] = Action { implicit request =>
    Ok(html.not_enrolled_template())
  }


  private def saReference(enrolments: List[Enrolment]): String = {
    val enrolment = enrolments.find(_.key == "IR-SA-AGENT").get
    val identifier = enrolment.identifiers.find(_.key == "IrAgentReference")
    identifier.get.value
  }
}
