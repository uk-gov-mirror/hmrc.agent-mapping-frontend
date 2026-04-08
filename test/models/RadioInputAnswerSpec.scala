/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer
import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer.No
import uk.gov.hmrc.agentmappingfrontend.model.RadioInputAnswer.Yes
import uk.gov.hmrc.agentmappingfrontend.support.UnitSpec
import uk.gov.hmrc.http.BadRequestException

class RadioInputAnswerSpec
extends UnitSpec:

  "RadioInputAnswer" should:
    "return Yes when input is 'yes'" in:
      RadioInputAnswer.apply("yes") shouldBe Yes
    "return No when inoput is 'no'" in:
      RadioInputAnswer.apply("no") shouldBe No
    "throw an exception when any other value is passed" in:
      an[BadRequestException] shouldBe thrownBy:
        RadioInputAnswer.apply("true")
    "return 'yes' when input is Yes" in:
      RadioInputAnswer.unapply(Yes) shouldBe Some("yes")

    "return 'no' when input is No" in:
      RadioInputAnswer.unapply(No) shouldBe Some("no")
