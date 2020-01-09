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

package uk.gov.hmrc.agentmappingfrontend.controllers.testOnly

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.views.html.{no_mappings, view_sa_mappings, view_vat_mappings}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class TestOnlyController @Inject()(
  override val messagesApi: MessagesApi,
  mappingConnector: MappingConnector,
  viewSaMappingsTemplate: view_sa_mappings,
  noMappingsTemplate: no_mappings,
  viewVatMappingsTemplate: view_vat_mappings)(
  implicit appConfig: AppConfig,
  val ec: ExecutionContext,
  cc: MessagesControllerComponents)
    extends FrontendController(cc) with I18nSupport {

  def findSaMappings(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    mappingConnector.findSaMappingsFor(arn).map { mappings =>
      if (mappings.nonEmpty)
        Ok(viewSaMappingsTemplate(arn, mappings))
      else
        NotFound(noMappingsTemplate(arn))
    }
  }

  def findVatMappings(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    mappingConnector.findVatMappingsFor(arn).map { mappings =>
      if (mappings.nonEmpty)
        Ok(viewVatMappingsTemplate(arn, mappings))
      else
        NotFound(noMappingsTemplate(arn))
    }
  }

  def deleteAllMappings(arn: Arn): Action[AnyContent] = Action.async { implicit request =>
    mappingConnector.deleteAllMappingsBy(arn).map { _ =>
      Ok(noMappingsTemplate(arn))
    }
  }
}
