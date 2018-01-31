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

package uk.gov.hmrc.agentmappingfrontend.model

import uk.gov.hmrc.play.test.UnitSpec

class IdentifierSpec extends UnitSpec {

  "Identifier" should {
    "parse a string having valid format" in {
      Identifier.parse("FOo~BaR") shouldBe Identifier("FOo", "BaR")
    }
    "throw an exception if string representation is not valid" in {
      an[IllegalArgumentException] shouldBe thrownBy(Identifier.parse(""))
      an[IllegalArgumentException] shouldBe thrownBy(Identifier.parse("FOoBar"))
      an[IllegalArgumentException] shouldBe thrownBy(Identifier.parse("FOo~Bar~123"))
    }
  }

}
