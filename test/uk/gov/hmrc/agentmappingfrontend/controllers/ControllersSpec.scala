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

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

class ControllersSpec extends UnitSpec {

  "prettify method for ARN" should {
    "return arn in expected format" in {
      prettify(Arn("TARN0000001")) shouldBe "TARN-000-0001"
    }

    "return exception when input arn is invalid" in {
      an[Exception] shouldBe thrownBy(prettify(Arn("TARN-0000001")))
    }
  }

  "normalizeArn" should {

    "return a valid arn when the input arn string is in hyphen pattern" in {
      normalizeArn("TARN-000-0001") shouldBe Some(Arn("TARN0000001"))
    }

    "return a valid arn when the input arn string is in default arn pattern" in {
      normalizeArn("TARN0000001") shouldBe Some(Arn("TARN0000001"))
    }

    "return None when the input arn string is in an invalid pattern" in {
      normalizeArn("TARN 00 0001") shouldBe None
    }

    "prettify method for Utr" should {
      "return utr in expected format" in {
        prettify(Utr("1097172564")) shouldBe "10971 72564"
      }

      "return exception when input arn is invalid" in {
        an[Exception] shouldBe thrownBy(prettify(Utr("1102696864.")))
      }
    }

    "normalizeUtr" should {

      "return a valid utr when the input utr string contains spaces" in {
        normalizeUtr("10 9    71 72564 ") shouldBe Some(Utr("1097172564"))
      }

      "return a valid utr when the input utr string is in default utr pattern" in {
        normalizeUtr("1097172564") shouldBe Some(Utr("1097172564"))
      }

      "return None when the input arn string is in an invalid pattern" in {
        normalizeUtr("1097172564.") shouldBe None
      }
    }
  }
}
