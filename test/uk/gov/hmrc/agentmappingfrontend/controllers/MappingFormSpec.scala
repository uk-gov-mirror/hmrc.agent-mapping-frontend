/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.controllers

import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.play.test.UnitSpec

class MappingFormSpec extends UnitSpec {

  "Arn MappingController.mappingFormArn" should {
    val arnForm = MappingController.mappingFormArn

    "valid arn bind" in {
      arnForm.bind(Map("arn.arn" -> "TARN0000001")).get.arn shouldBe Arn("TARN0000001")
    }

    "invalid arn reject" in {
      arnForm.bind(Map("arn.arn" -> "invalidArn")).errors.head.messages.head shouldBe "error.arn.invalid"
    }
  }

  "Utr MappingController.mappingFormUtr" should {
    val utrForm = MappingController.mappingFormUtr

    "valid utr bind" in {
      utrForm.bind(Map("utr.value" -> "2000000000")).get.utr shouldBe Utr("2000000000")
    }

    "invalid utr reject" in {
      utrForm.bind(Map("utr.value" -> "invalidUtr")).errors.head.messages.head shouldBe "error.utr.invalid"
    }

    "Utr Blank" in {
      utrForm.bind(Map("utr.value" -> "")).errors.head.messages.head shouldBe "error.utr.blank"
    }
  }
}
