package uk.gov.hmrc.agentmappingfrontend.repository

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentmappingfrontend.support.MongoApp
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class MappingArnRepositoryISpec extends UnitSpec with GuiceOneAppPerSuite with MongoApp {

  protected def builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(mongoConfiguration)

  override implicit lazy val app: Application = builder.build()

  private lazy val repo = app.injector.instanceOf[MappingArnRepository]

  override def beforeEach() {
    super.beforeEach()
    await(repo.ensureIndexes)
    ()
  }
  private val arn = Arn("TARN0000001")

  "MappingArnRepository" should {

    "create a MappingArnResult record" in {
      val result = await(repo.create(arn))
      result should not be empty

      val mappingArnResult = await(repo.find("id" -> result)).head
      mappingArnResult should have('id (result), 'arn (arn))
      mappingArnResult.id.size shouldBe 32
    }

    "find a MappingArnResult record by Id" in {
      val record = MappingArnResult(arn, 0, Seq.empty)
      await(repo.insert(record))

      val result = await(repo.findRecord(record.id))

      result shouldBe Some(record)
    }

    "update client count and ggTag" in {
      val record = MappingArnResult(arn, 0, Seq.empty)

      await(repo.insert(record))
      await(repo.upsert(record.copy(clientCountAndGGTags = record.clientCountAndGGTags :+ ClientCountAndGGTag(12,"")), record.id))
      val result = await(repo.findRecord(record.id).get.clientCountAndGGTags.head.clientCount)

      result shouldBe 12
    }

    "update current ggTag" in {
      val record = MappingArnResult(arn, 0, Seq.empty)

      await(repo.insert(record))
      await(repo.updateCurrentGGTag(record.id, "6666"))
      val result = await(repo.findRecord(record.id).get.currentGGTag)

      result shouldBe "6666"
    }

    "update mapping complete status to true" in {
      val record = MappingArnResult(arn, 0, Seq.empty)

      await(repo.insert(record))
      await(repo.updateMappingCompleteStatus(record.id))
      val result = await(repo.findRecord(record.id).get.alreadyMapped)

      result shouldBe true
    }


    "delete a MappingArnResult record by Id" in {
      val record = MappingArnResult(arn, 0, Seq.empty)
      await(repo.insert(record))

      await(repo.delete(record.id))

      await(repo.find("id" -> record.id)) shouldBe empty
    }
  }
}
