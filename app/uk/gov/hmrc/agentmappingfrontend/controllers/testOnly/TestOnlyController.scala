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

package uk.gov.hmrc.agentmappingfrontend.controllers.testOnly

import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.MessagesRequest
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.views.html.no_mappings
import uk.gov.hmrc.agentmappingfrontend.views.html.view_sa_mappings
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestOnlyController @Inject() (
  override val messagesApi: MessagesApi,
  mappingConnector: MappingConnector,
  viewSaMappingsTemplate: view_sa_mappings,
  noMappingsTemplate: no_mappings
)(implicit
  val ec: ExecutionContext,
  cc: MessagesControllerComponents
)
extends FrontendController(cc)
with I18nSupport:

  def findSaMappings(arn: Arn): Action[AnyContent] = Action.async: request =>
    given MessagesRequest[?] = request
    mappingConnector.findSaMappingsFor(arn).map { mappings =>
      if mappings.nonEmpty then
        Ok(viewSaMappingsTemplate(arn, mappings))
      else
        NotFound(noMappingsTemplate(arn))
    }

  def deleteAllMappings(arn: Arn): Action[AnyContent] = Action.async: request =>
    given MessagesRequest[?] = request
    mappingConnector.deleteAllMappingsBy(arn).map { _ =>
      Ok(noMappingsTemplate(arn))
    }
