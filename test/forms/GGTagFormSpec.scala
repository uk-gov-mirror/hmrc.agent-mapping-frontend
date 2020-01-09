/*
 * Copyright 2020 HM Revenue & Customs
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

package forms

import uk.gov.hmrc.agentmappingfrontend.model.GGTagForm
import uk.gov.hmrc.play.test.UnitSpec

class GGTagFormSpec extends UnitSpec {

  "ggtagForm" should {

    "have no errors when ggTag is valid with only digits" in {
      val form = GGTagForm.form.bind(Map("ggTag" -> "1234"))
      form.hasErrors shouldBe false
    }

    "have no errors when ggTag is valid with only alphabets" in {
      val form = GGTagForm.form.bind(Map("ggTag" -> "abcd"))
      form.hasErrors shouldBe false
    }

    "be case in-sensitive" in {
      val form = GGTagForm.form.bind(Map("ggTag" -> "ABcd"))
      form.hasErrors shouldBe false
    }

    "have errors when ggTag is invalid" in {
      val form = GGTagForm.form.bind(Map("ggTag" -> "ab_&"))
      form.hasErrors shouldBe true
      form.errors.head.message shouldBe "error.gg-tag.invalid"
    }

    "have errors when ggTag has more than 4 characters" in {
      val form = GGTagForm.form.bind(Map("ggTag" -> "ab12AS"))
      form.hasErrors shouldBe true
      form.errors.head.message shouldBe "error.gg-tag.invalid"
    }

    "have errors when ggTag has less than 4 characters" in {
      val form = GGTagForm.form.bind(Map("ggTag" -> "aA1"))
      form.hasErrors shouldBe true
      form.errors.head.message shouldBe "error.gg-tag.invalid"
    }
  }

}
