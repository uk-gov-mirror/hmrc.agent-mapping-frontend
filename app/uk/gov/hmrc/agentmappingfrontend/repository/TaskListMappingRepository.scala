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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

case class TaskListMappingResult(
  id: MappingArnResultId,
  continueId: String,
  clientCount: Int = 0,
  createdDate: DateTime = DateTime.now(DateTimeZone.UTC),
  alreadyMapped: Boolean = false
)

object TaskListMappingResult {

  def apply(continueId: String): TaskListMappingResult = {
    val id: MappingArnResultId = UUID.randomUUID().toString.replace("-", "")
    TaskListMappingResult(id = id, continueId = continueId)
  }

  implicit val format: OFormat[TaskListMappingResult] = Json.format[TaskListMappingResult]
}

@Singleton
class TaskListMappingRepository @Inject()(appConfig: AppConfig, mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[TaskListMappingResult, BSONObjectID](
      "mapping-task-list",
      mongoComponent.mongoConnector.db,
      TaskListMappingResult.format,
      ReactiveMongoFormats.objectIdFormats) {

  def findRecord(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Option[TaskListMappingResult]] =
    find("id" -> id).map(_.headOption)

  override def indexes: Seq[Index] =
    Seq(
      Index(key = Seq("id" -> IndexType.Ascending), name = Some("idUnique"), unique = true),
      Index(
        key = Seq("createdDate" -> IndexType.Ascending),
        name = Some("createDate"),
        unique = false,
        options = BSONDocument("expireAfterSeconds" -> 86400)
      )
    )

  def create(continueId: String)(implicit ec: ExecutionContext): Future[MappingArnResultId] = {

    val newRecord = TaskListMappingResult(continueId)
    findByContinueId(continueId).flatMap {
      case Some(record) => delete(record.id).flatMap(_ => insert(newRecord).map(_ => newRecord.id))
      case None         => insert(newRecord).map(_ => newRecord.id)
    }
  }

  def findByContinueId(continueId: String)(implicit ec: ExecutionContext): Future[Option[TaskListMappingResult]] =
    find("continueId" -> continueId).map(_.headOption)

  def updateFor(id: MappingArnResultId, clientCount: Int)(implicit ec: ExecutionContext): Future[Unit] =
    findRecord(id).flatMap {
      case Some(record) => {
        val updatedClientCount = clientCount
        val updatedRecord = Json.toJson(record.copy(clientCount = updatedClientCount)).as[JsObject]

        findAndUpdate(
          Json.obj("id" -> id),
          updatedRecord
        ).map(_ => ())
      }
      case _ => throw new RuntimeException(s"could not find record with id $id")
    }

  def upsert(taskListMappingResult: TaskListMappingResult, continueId: String)(
    implicit ec: ExecutionContext): Future[Unit] =
    collection
      .update(ordered = false)
      .one(Json.obj("continueId" -> continueId), taskListMappingResult, upsert = true)
      .checkResult

  private implicit class WriteResultChecker(future: Future[WriteResult]) {
    def checkResult(implicit ec: ExecutionContext): Future[Unit] = future.map { writeResult =>
      if (hasProblems(writeResult)) throw new RuntimeException(writeResult.toString)
      else ()
    }
  }

  def delete(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Unit] =
    remove("id" -> id).map(_ => ())

  private def hasProblems(writeResult: WriteResult): Boolean =
    !writeResult.ok || writeResult.writeErrors.nonEmpty || writeResult.writeConcernError.isDefined
}
