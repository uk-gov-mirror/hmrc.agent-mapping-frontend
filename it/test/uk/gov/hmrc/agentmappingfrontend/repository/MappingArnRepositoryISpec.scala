/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mongodb.scala.model.Filters
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.support.UnitSpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.SECONDS
import scala.concurrent.ExecutionContext.Implicits.global

class MappingArnRepositoryISpec
extends UnitSpec
with GuiceOneAppPerSuite
with DefaultPlayMongoRepositorySupport[MappingArnResult] {

  protected def builder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> mongoUri)

  override implicit lazy val app: Application = builder.build()

  val repository: PlayMongoRepository[MappingArnResult] = new MappingArnRepository(mongoComponent)

  val mappingArnRepository: MappingArnRepository = repository.asInstanceOf[MappingArnRepository]

  private val arn = Arn("TARN0000001")

  "MappingArnRepository" should {

    "create a MappingArnResult record" in {
      val now = Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime.truncatedTo(MILLIS)
      val result = await(mappingArnRepository.create(arn))
      result should not be empty

      val mappingArnResult = await(repository.collection.find(Filters.equal("id", result)).head())
      mappingArnResult should have(Symbol("id")(result), Symbol("arn")(arn))
      mappingArnResult.id.size shouldBe 32
      mappingArnResult.createdDate.toString.length shouldBe now.toString.length // check precision
      mappingArnResult.createdDate.truncatedTo(SECONDS) shouldBe now.truncatedTo(
        SECONDS
      ) // check approx match (await time results in ms delay), could remove
      mappingArnResult.currentCount shouldBe 0
    }

    "find a MappingArnResult record by Id" in {
      val record = MappingArnResult(
        arn,
        0,
        Seq.empty
      )
      await(repository.collection.insertOne(record).toFuture())

      val result = await(mappingArnRepository.findRecord(record.id))

      result shouldBe Some(record)
    }

    "update client count and ggTag" in {
      val record = MappingArnResult(
        arn,
        0,
        Seq.empty
      )

      await(repository.collection.insertOne(record).toFuture())
      await(
        mappingArnRepository.upsert(
          record.copy(clientCountAndGGTags = record.clientCountAndGGTags :+ ClientCountAndGGTag(12, "")),
          record.id
        )
      )
      val result = await(mappingArnRepository.findRecord(record.id)).get.clientCountAndGGTags.head.clientCount

      result shouldBe 12
    }

    "update current ggTag" in {
      val record = MappingArnResult(
        arn,
        0,
        Seq.empty
      )

      await(repository.collection.insertOne(record).toFuture())
      await(mappingArnRepository.updateCurrentGGTag(record.id, "6666"))
      val result = await(mappingArnRepository.findRecord(record.id)).get.currentGGTag

      result shouldBe "6666"
    }

    "update mapping complete status to true" in {
      val record = MappingArnResult(
        arn,
        0,
        Seq.empty
      )

      await(repository.collection.insertOne(record).toFuture())
      await(mappingArnRepository.updateMappingCompleteStatus(record.id))
      val result = await(mappingArnRepository.findRecord(record.id)).get.alreadyMapped

      result shouldBe true
    }

    "delete a MappingArnResult record by Id" in {
      val record = MappingArnResult(
        arn,
        0,
        Seq.empty
      )
      await(repository.collection.insertOne(record).toFuture())

      await(mappingArnRepository.delete(record.id))

      await(repository.collection.find(Filters.equal("id", record.id)).headOption()) shouldBe empty
    }
  }

}
