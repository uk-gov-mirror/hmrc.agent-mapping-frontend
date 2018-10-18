/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.controllers.testOnly

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmappingfrontend.views.html.{no_mappings, view_sa_mappings, view_vat_mappings}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

class TestOnlyController @Inject()(override val messagesApi: MessagesApi, mappingConnector: MappingConnector)(
  implicit appConfig: AppConfig)
    extends FrontendController with I18nSupport {

  def findSaMappings(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    mappingConnector.findSaMappingsFor(arn).map { mappings =>
      if (mappings.nonEmpty)
        Ok(view_sa_mappings(arn, mappings))
      else
        NotFound(no_mappings(arn))
    }
  }

  def findVatMappings(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    mappingConnector.findVatMappingsFor(arn).map { mappings =>
      if (mappings.nonEmpty)
        Ok(view_vat_mappings(arn, mappings))
      else
        NotFound(no_mappings(arn))
    }
  }

  def deleteAllMappings(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    mappingConnector.deleteAllMappingsBy(arn).map { _ =>
      Ok(no_mappings(arn))
    }
  }
}
