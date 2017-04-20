/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.model

import uk.gov.hmrc.play.test.UnitSpec

class UtrSpec extends UnitSpec {
  "isValid" should {
    "be true for a valid UTR" in {
      Utr.isValid("2000000000") shouldBe true
    }

    "be false" when {
      "it has more than 10 digits" in {
        Utr.isValid("20000000000") shouldBe false
      }

      "it has fewer than 10 digits" in {
        Utr.isValid("200000") shouldBe false
      }

      "it has non-digit characters" in {
        Utr.isValid("200000000B") shouldBe false
      }

      "it has non-alphanumeric characters" in {
        Utr.isValid("200000000!") shouldBe false
      }

      "be false when the checksum doesn't pass" in {
        Utr.isValid("2000000001") shouldBe false
      }
    }
  }
}
