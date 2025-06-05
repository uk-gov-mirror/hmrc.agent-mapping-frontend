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

package uk.gov.hmrc.agentmappingfrontend.repository

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.FindOneAndReplaceOptions
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.ReplaceOptions
import play.api.Logging
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentmappingfrontend.model.MongoLocalDateTimeFormat
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class TaskListMappingResult(
  id: MappingArnResultId,
  continueId: String,
  clientCount: Int = 0,
  createdDate: LocalDateTime = Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime,
  alreadyMapped: Boolean = false
)

object TaskListMappingResult {

  def apply(continueId: String): TaskListMappingResult = {
    val id: MappingArnResultId = UUID.randomUUID().toString.replace("-", "")
    TaskListMappingResult(id = id, continueId = continueId)
  }

  implicit val localDateTimeFormat: Format[LocalDateTime] = MongoLocalDateTimeFormat.localDateTimeFormat
  implicit val format: OFormat[TaskListMappingResult] = Json.format[TaskListMappingResult]

}

@Singleton
class TaskListMappingRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
extends PlayMongoRepository[TaskListMappingResult](
  mongoComponent = mongoComponent,
  collectionName = "mapping-task-list",
  domainFormat = TaskListMappingResult.format,
  indexes = Seq(
    IndexModel(ascending("id"), IndexOptions().name("idUnique").unique(true)),
    IndexModel(
      ascending("createdDate"),
      IndexOptions().name("createdDate").unique(false).expireAfter(86400, TimeUnit.SECONDS)
    )
  ),
  replaceIndexes = true
)
with Logging {

  private val ID = "id"
  private val CONTINUE_ID = "continueId"
  private val CLIENT_COUNT = "clientCount"

  def findRecord(id: MappingArnResultId): Future[Option[TaskListMappingResult]] = collection
    .find(equal(ID, id))
    .headOption()

  def create(continueId: String): Future[MappingArnResultId] = {
    val newRecord = TaskListMappingResult(continueId)
    collection
      .replaceOne(
        equal(CONTINUE_ID, continueId),
        newRecord,
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => newRecord.id)
  }

  def findByContinueId(continueId: String): Future[Option[TaskListMappingResult]] = collection
    .find(equal(CONTINUE_ID, continueId))
    .headOption()

  def updateFor(
    id: MappingArnResultId,
    clientCount: Int
  ): Future[Unit] = collection
    .updateOne(equal(ID, id), set(CLIENT_COUNT, clientCount))
    .toFuture()
    .map(ur =>
      logger.info(
        s"UpdateFor was acknowledged: ${ur.wasAcknowledged()}. " +
          s"Matched documents: ${ur.getMatchedCount}. Updated count: ${ur.getModifiedCount}"
      )
    )

  def upsert(
    taskListMappingResult: TaskListMappingResult,
    continueId: String
  ): Future[Unit] = collection
    .findOneAndReplace(
      equal(CONTINUE_ID, continueId),
      taskListMappingResult,
      FindOneAndReplaceOptions().upsert(true)
    )
    .toFuture()
    .map(_ => ())

  def delete(id: MappingArnResultId): Future[Unit] = collection
    .deleteOne(equal(ID, id))
    .toFuture()
    .map(_ => ())

}
