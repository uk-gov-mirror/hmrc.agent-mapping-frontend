package uk.gov.hmrc.agentmappingfrontend.repository

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentmappingfrontend.support.MongoApp
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class MappingArnRepositoryISpec extends UnitSpec with OneAppPerSuite with MongoApp {

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

    "find a MappingArnResult ARN value by Id" in {
      val record = MappingArnResult(arn, 0)
      await(repo.insert(record))

      val result = await(repo.findArn(record.id))

      result shouldBe Some(record.arn)
    }

    "find a MappingArnResult record by Id" in {
      val record = MappingArnResult(arn, 0)
      await(repo.insert(record))

      val result = await(repo.findRecord(record.id))

      result shouldBe Some(record)
    }

    "update client count" in {
      val record = MappingArnResult(arn, 0)

      await(repo.insert(record))
      await(repo.updateFor(record.id,12))
      val result = await(repo.findRecord(record.id).get.clientCount)

      result shouldBe 12
    }


    "delete a MappingArnResult record by Id" in {
      val record = MappingArnResult(arn, 0)
      await(repo.insert(record))

      await(repo.delete(record.id))

      await(repo.find("id" -> record.id)) shouldBe empty
    }

    "not return any MappingArnResult record for an invalid Id" in {
      val result = await(repo.findArn("INVALID"))

      result shouldBe empty
    }
  }
}
