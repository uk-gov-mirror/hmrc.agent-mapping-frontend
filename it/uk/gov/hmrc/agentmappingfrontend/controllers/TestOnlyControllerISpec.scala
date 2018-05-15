package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.testOnly.TestOnlyController
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs._

class TestOnlyControllerISpec extends BaseControllerISpec {

  private lazy val controller: TestOnlyController =
    app.injector.instanceOf[TestOnlyController]

  val arn = Arn("ARN0001")
  val sa = "AgentCode1"
  val vrn = "101747696"

  "findMappings" should {
    "return OK with list of sa mappings" in {
      saMappingsFound(arn)
      val response = await(controller.findSaMappings(arn)(FakeRequest()))
      status(response) shouldBe 200
      val html = bodyOf(response)
      html should include(arn.value)
      html should include(sa)
    }

    "return Not Found when given arn does not have any sa mappings" in {
      noSaMappingsFound(arn)
      val response = await(controller.findSaMappings(arn)(FakeRequest()))

      status(response) shouldBe 404
      bodyOf(response) should include(s"No mappings found for ${arn.value}")
    }

    "return OK with list of vat mappings" in {
      vatMappingsFound(arn)
      val response = await(controller.findVatMappings(arn)(FakeRequest()))
      status(response) shouldBe 200
      val html = bodyOf(response)
      html should include(arn.value)
      html should include(vrn)
    }

    "return Not Found when given arn does not have any vat mappings" in {
      noVatMappingsFound(arn)
      val response = await(controller.findVatMappings(arn)(FakeRequest()))

      status(response) shouldBe 404
      bodyOf(response) should include(s"No mappings found for ${arn.value}")
    }
  }

  "delete" should {
    "return OK when given Arn to delete mapping" in {
      mappingsDelete(arn)
      val response = await(controller.deleteAllMappings(arn)(FakeRequest()))
      status(response) shouldBe 200
      val html = bodyOf(response)
      html should include(arn.value)
    }
  }
}
