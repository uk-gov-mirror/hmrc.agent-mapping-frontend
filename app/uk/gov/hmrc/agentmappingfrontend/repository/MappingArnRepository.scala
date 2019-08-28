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
import play.api.libs.json.{Format, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

case class ClientCountAndGGTag(clientCount: Int, ggTag: String)

case object ClientCountAndGGTag {
  implicit val formats: OFormat[ClientCountAndGGTag] = Json.format
}

case class MappingArnResult(
  id: MappingArnResultId,
  arn: Arn,
  createdDate: DateTime = DateTime.now(DateTimeZone.UTC),
  currentCount: Int,
  currentGGTag: String = "",
  clientCountAndGGTags: Seq[ClientCountAndGGTag] = Seq.empty,
  alreadyMapped: Boolean = false)

object MappingResult {
  type MappingArnResultId = String

}

object MappingArnResult {

  def apply(arn: Arn, currentCount: Int, clientCountAndGGTags: Seq[ClientCountAndGGTag]): MappingArnResult = {
    val id: MappingArnResultId = UUID.randomUUID().toString.replace("-", "")
    MappingArnResult(id = id, arn = arn, currentCount = currentCount, clientCountAndGGTags = clientCountAndGGTags)
  }

  implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
  implicit val format: OFormat[MappingArnResult] = Json.format
}

@Singleton
class MappingArnRepository @Inject()(appConfig: AppConfig, mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[MappingArnResult, BSONObjectID](
      "mapping-arn",
      mongoComponent.mongoConnector.db,
      MappingArnResult.format,
      ReactiveMongoFormats.objectIdFormats) {

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

  def create(arn: Arn, currentCount: Int = 0, clientCountAndGGTags: Seq[ClientCountAndGGTag] = Seq.empty)(
    implicit ec: ExecutionContext): Future[MappingArnResultId] = {
    val record = MappingArnResult(arn = arn, currentCount = currentCount, clientCountAndGGTags = clientCountAndGGTags)
    insert(record).map(_ => record.id)
  }

  def findRecord(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Option[MappingArnResult]] =
    find("id" -> id).map(_.headOption)

  def updateClientCountAndGGTag(id: MappingArnResultId, clientCountAndGGTag: ClientCountAndGGTag)(
    implicit ec: ExecutionContext): Future[Unit] = {
    val updateOp = Json.obj("$addToSet" -> Json.obj("clientCountAndGGTags" -> clientCountAndGGTag))
    collection.update(ordered = false).one(Json.obj("id" -> id), updateOp).checkResult
  }

  def updateCurrentGGTag(id: MappingArnResultId, ggTag: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val updateOp = Json.obj("$set" -> Json.obj("currentGGTag" -> ggTag))
    collection.update(ordered = false).one(Json.obj("id" -> id), updateOp).checkResult
  }

  def updateMappingCompleteStatus(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Unit] = {
    val updateOp = Json.obj("$set" -> Json.obj("alreadyMapped" -> true))
    collection.update(ordered = false).one(Json.obj("id" -> id), updateOp).checkResult
  }

  def delete(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Unit] =
    remove("id" -> id).map(_ => ())

  implicit class WriteResultChecker(future: Future[WriteResult]) {
    def checkResult(implicit ec: ExecutionContext): Future[Unit] = future.map { writeResult =>
      if (hasProblems(writeResult)) throw new RuntimeException(writeResult.toString)
      else ()
    }
  }

  private def hasProblems(writeResult: WriteResult): Boolean =
    !writeResult.ok || writeResult.writeErrors.nonEmpty || writeResult.writeConcernError.isDefined
}
