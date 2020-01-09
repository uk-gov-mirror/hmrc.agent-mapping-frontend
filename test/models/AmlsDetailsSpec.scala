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

package models

import java.time.LocalDate

import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model.{AmlsDetails, PendingDetails, RegisteredDetails}
import uk.gov.hmrc.play.test.UnitSpec

class AmlsDetailsSpec extends UnitSpec {

  "AmlsDetails" should {
    "serialize to json" when {
      "pending amls details" in {
        Json.toJson(AmlsDetails("supervisory", Left(PendingDetails(LocalDate.parse("2019-01-01"))))) shouldBe
          Json.parse("""{"supervisoryBody": "supervisory", "appliedOn": "2019-01-01"}""")
      }
      "registered amls details" in {
        Json.toJson(
          AmlsDetails("supervisory", Right(RegisteredDetails("memNumber-123", LocalDate.parse("2020-10-10"))))) shouldBe
          Json.parse(
            """{"supervisoryBody": "supervisory", "membershipNumber": "memNumber-123", "membershipExpiresOn": "2020-10-10"}""")
      }
    }

    "deserialize from json" when {
      "pending amls details" in {
        Json.parse("""{"supervisoryBody": "supervisory", "appliedOn": "2019-01-01"}""").as[AmlsDetails] shouldBe
          AmlsDetails("supervisory", Left(PendingDetails(LocalDate.parse("2019-01-01"))))
      }

      "registered amls details" in {
        Json
          .parse(
            """{"supervisoryBody": "supervisory", "membershipNumber": "memNumber-123", "membershipExpiresOn": "2020-10-10"}""")
          .as[AmlsDetails] shouldBe
          AmlsDetails("supervisory", Right(RegisteredDetails("memNumber-123", LocalDate.parse("2020-10-10"))))
      }
    }
  }
}
