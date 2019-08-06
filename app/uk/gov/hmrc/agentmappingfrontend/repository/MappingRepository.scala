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

package uk.gov.hmrc.agentmappingfrontend.repository

import play.api.libs.json.{Format, OFormat}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

abstract class MappingRepository[A, ID](collectionName: String, reactiveMongoComponent: ReactiveMongoComponent)(
  implicit manifest: Manifest[A],
  mid: Manifest[ID],
  domainFormat: OFormat[A],
  idFormat: Format[ID])
    extends ReactiveRepository[A, ID](collectionName, reactiveMongoComponent.mongoConnector.db, domainFormat, idFormat) {

  def findRecord(id: ID)(implicit ec: ExecutionContext): Future[Option[A]] =
    find("id" -> id).map(_.headOption)

}
