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
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.ReplaceOptions
import play.api.Logging
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentmappingfrontend.model.LegacyClientDetails
import uk.gov.hmrc.agentmappingfrontend.model.MongoLocalDateTimeFormat
import uk.gov.hmrc.agentmappingfrontend.repository.MappingResult.MappingArnResultId
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.temporal.ChronoUnit.MILLIS
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class ClientCountAndGGTag(
  clientCount: Int,
  ggTag: String
)

case object ClientCountAndGGTag {
  implicit val formats: OFormat[ClientCountAndGGTag] = Json.format
}

case class MappingArnResult(
  id: MappingArnResultId = UUID.randomUUID().toString.replace("-", ""),
  arn: Arn,
  legacyClientDetails: Option[LegacyClientDetails] = None,
  agentCode: Option[String] = None,
  mappedAgentCode: Option[String] = None,
  mappedClientCount: Option[Int] = None,
  createdDate: LocalDateTime = Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime.truncatedTo(MILLIS)
)

object MappingResult {
  type MappingArnResultId = String
}

object MappingArnResult {

  implicit val localDateTimeFormat: Format[LocalDateTime] = MongoLocalDateTimeFormat.localDateTimeFormat
  implicit val format: OFormat[MappingArnResult] = Json.format

}

@Singleton
class MappingArnRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
extends PlayMongoRepository[MappingArnResult](
  mongoComponent = mongoComponent,
  collectionName = "mapping-arn",
  domainFormat = MappingArnResult.format,
  indexes = Seq(
    IndexModel(ascending("id"), IndexOptions().name("idUnique").unique(true)),
    IndexModel(
      ascending("createdDate"),
      IndexOptions().name("createDate").unique(false).expireAfter(86400, TimeUnit.SECONDS)
    )
  ),
  replaceIndexes = true
)
with Logging {

  def create(
    arn: Arn,
    optLegacyClientDetails: Option[LegacyClientDetails] = None
  ): Future[MappingArnResultId] = {
    val record = MappingArnResult(
      arn = arn,
      legacyClientDetails = optLegacyClientDetails
    )
    collection
      .insertOne(record)
      .toFuture()
      .map(_ => record.id)
  }

  def findRecord(id: MappingArnResultId): Future[Option[MappingArnResult]] = collection
    .find(equal("id", id))
    .headOption()

  def replace(
    mappingArnResult: MappingArnResult,
    id: MappingArnResultId
  ): Future[Unit] = collection
    .replaceOne(
      equal("id", id),
      mappingArnResult,
      ReplaceOptions()
        .upsert(true)
    )
    .toFuture()
    .map(wr =>
      if (!wr.wasAcknowledged())
        throw new RuntimeException("Something went wrong with upsert.")
      else
        logger.info(
          s"Upsert success. Found ${wr.getMatchedCount} matching documents. " +
            s"${wr.getModifiedCount} were modified."
        )
    )

  def delete(id: MappingArnResultId): Future[Unit] = collection
    .deleteOne(equal("id", id))
    .toFuture()
    .map(_ => ())

}
