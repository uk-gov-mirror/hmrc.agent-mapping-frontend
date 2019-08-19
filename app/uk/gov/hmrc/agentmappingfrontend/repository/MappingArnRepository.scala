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
import play.api.libs.json.{JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import reactivemongo.play.json.ImplicitBSONHandlers._

import scala.concurrent.{ExecutionContext, Future}

case class MappingArnResult(
  id: MappingArnResultId,
  arn: Arn,
  createdDate: DateTime = DateTime.now(DateTimeZone.UTC),
  clientCount: Int = 0,
  ggTag: String = "",
  alreadyMapped: Boolean = false)

object MappingResult {
  type MappingArnResultId = String

}

object MappingArnResult {
  //type MappingArnResultId  = String

  def apply(arn: Arn, count: Int): MappingArnResult = {
    val id: MappingArnResultId = UUID.randomUUID().toString.replace("-", "")
    MappingArnResult(id = id, arn = arn, clientCount = count)
  }

  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val format = Json.format[MappingArnResult]
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

  def findArn(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Option[Arn]] =
    find("id" -> id).map(_.headOption.map(_.arn))

  def findRecord(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Option[MappingArnResult]] =
    find("id" -> id).map(_.headOption)

  def create(arn: Arn, clientCount: Int = 0)(implicit ec: ExecutionContext): Future[MappingArnResultId] = {
    val record = MappingArnResult(arn = arn, count = clientCount)
    insert(record).map(_ => record.id)
  }

  def updateFor(id: MappingArnResultId, clientCount: Int)(implicit ec: ExecutionContext): Future[Unit] =
    findRecord(id).flatMap {
      case Some(record) =>
        val updatedClientCount = clientCount
        val updatedRecord = Json.toJson(record.copy(clientCount = updatedClientCount)).as[JsObject]

        findAndUpdate(
          Json.obj("id" -> id),
          updatedRecord
        ).map(_ => ())

      case _ => throw new RuntimeException(s"could not find record with id $id")
    }

  def updateFor(id: MappingArnResultId)(implicit ec: ExecutionContext): Future[Unit] =
    findRecord(id).flatMap {
      case Some(record) => {
        val updatedRecord = Json.toJson(record.copy(alreadyMapped = true)).as[JsObject]

        findAndUpdate(
          Json.obj("id" -> id),
          updatedRecord
        ).map(_ => ())
      }
      case None => throw new RuntimeException(s"could not update record for id $id")
    }

  def updateGGTag(id: MappingArnResultId, ggTag: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val updateOp = Json.obj("$set" -> (Json.obj("ggTag" -> ggTag)))
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
