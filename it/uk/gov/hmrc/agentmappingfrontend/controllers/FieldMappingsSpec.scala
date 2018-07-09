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

    "give \"error.utr.invalid.length\" error when it has wrong long length" in {
      bind("20000000000").left.value should contain only FormError("testKey", "error.utr.invalid.length")
      bind("20000000 0").left.value should contain only FormError("testKey", "error.utr.invalid.length")
      bind("20 00000 0").left.value should contain only FormError("testKey", "error.utr.invalid.length")
    }

    "give \"error.utr.invalid.length\" error when it has wrong short length" in {
      bind("20000").left.value should contain only FormError("testKey", "error.utr.invalid.length")
    }

    "give \"error.utr.invalid.format\" error when it has wrong format" in {
      bind("2000000.0000").left.value should contain only FormError("testKey", "error.utr.invalid.format")
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

    "give \"error.arn.blank\" error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", "error.arn.blank")
    }

    "give \"error.arn.blank\" error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", "error.arn.blank")
    }

    "give \"error.arn.invalid\" error when it is invalid" in {
      bind("ARN0000001").left.value should contain only FormError("testKey", "error.arn.invalid")
    }

    "give \"error.arn.invalid\" error when it is invalid with hyphens in different places" in {
      bind("TARN-0000-001").left.value should contain only FormError("testKey", "error.arn.invalid")
    }
  }
}
