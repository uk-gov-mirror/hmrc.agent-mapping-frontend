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

import play.api.libs.json.{JsNumber, JsResultException, JsString, Json}
import uk.gov.hmrc.agentmappingfrontend.model.DateOfBirth
import uk.gov.hmrc.play.test.UnitSpec

class DateOfBirthSpec extends UnitSpec {

  val validDate: LocalDate = LocalDate.parse("2019-01-01")

  "DateOfBirth" should {

    "write to json" should {
      "successfully serialize date of birth to json" in {
        Json.toJson(DateOfBirth(validDate)) shouldBe JsString("2019-01-01")
      }
    }

    "read from json" should {
      "successfully deserialize date of birth from json" in {
        JsString("2019-01-01").as[DateOfBirth] shouldBe DateOfBirth(validDate)
      }

      "throw a JsResultException when date can not be formatted into yyyy-MM-dd" in {
        val exception = intercept[JsResultException] {
          JsString("01-01-2019").as[DateOfBirth]
        }
        val errorMessage: String = exception.errors.head._2.head.message
        errorMessage shouldBe "Could not parse date as yyyy-MM-dd: Text '01-01-2019' could not be parsed at index 0"
      }

      "throw a JsResultException when json is not of type JsString" in {
        val exception = intercept[JsResultException] {
          JsNumber(20190101).as[DateOfBirth]
        }
        val errorMessage: String = exception.errors.head._2.head.message
        errorMessage shouldBe "Expected string but got 20190101"
      }
    }
  }
}
