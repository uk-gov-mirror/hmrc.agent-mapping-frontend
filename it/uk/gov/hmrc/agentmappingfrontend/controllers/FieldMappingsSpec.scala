package uk.gov.hmrc.agentmappingfrontend.controllers

import org.scalatest.EitherValues
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class FieldMappingsSpec extends UnitSpec with EitherValues {
  "utr bind" should {
    val utrMapping = utr.withPrefix("testKey")

    def bind(fieldValue: String) = utrMapping.bind(Map("testKey" -> fieldValue))

    "accept valid UTRs" in {
      bind("2000000000") shouldBe Right("2000000000")
    }

    "unreachable by user, Give \"error.required\" error if somehow supplied Map.empty" in {
      utrMapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
    }

    "give \"error.required\" error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", "error.utr.blank")
    }

    "give \"error.required\" error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", "error.utr.blank")
    }

    "give \"error.utr.invalid\" error when it is invalid" in {
      bind("20000000000").left.value should contain only FormError("testKey", "error.utr.invalid")
    }
  }

  "arn bind" should {
    val arnMapping = arn.withPrefix("testKey")

    def bind(fieldValue: String) = arnMapping.bind(Map("testKey" -> fieldValue))

    "accept valid ARN" in {
      bind("TARN0000001") shouldBe Right("TARN0000001")
    }

    "give \"error.required\" error when it is not supplied" in {
      arnMapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
    }

    "give \"error.required\" error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", "error.required")
    }

    "give \"error.required\" error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", "error.required")
    }

    "give \"error.arn.invalid\" error when it is invalid" in {
      bind("ARN0000001").left.value should contain only FormError("testKey", "error.arn.invalid")
    }
  }
}
