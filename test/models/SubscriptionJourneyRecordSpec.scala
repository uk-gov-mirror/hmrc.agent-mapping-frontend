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

import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model._
import uk.gov.hmrc.agentmappingfrontend.support.UnitSpec
import uk.gov.hmrc.agentmtdidentifiers.model.Utr

import java.time.LocalDate

class SubscriptionJourneyRecordSpec
extends UnitSpec {
  // here mostly just to satisfy the coverage checks...
  "SubscriptionJourneyRecord" should {
    "serialize and deserialise correctly" in {
      val sjr: SubscriptionJourneyRecord = SubscriptionJourneyRecord(
        authProviderId = AuthProviderId("12345"),
        continueId = Some("continueId"), // once allocated, should not be changed?
        businessDetails = BusinessDetails(
          BusinessType.SoleTrader,
          Utr("utr"),
          Postcode("SW1 1AA"),
          Some(
            Registration(
              Some("taxpayer"),
              false,
              false,
              BusinessAddress(
                "1 Any st",
                None,
                None,
                None,
                Some("W1N2 2TD"),
                "GB"
              ),
              Some("email@email.com"),
              Some("01273111111")
            )
          ),
          None,
          None,
          None,
          None
        ),
        amlsData = Some(
          AmlsData(
            true,
            Some(false),
            Some(AmlsDetails(
              "body",
              Some("foo"),
              None,
              None,
              None,
              Some(LocalDate.now())
            ))
          )
        )
      )
      Json.toJson(sjr).as[SubscriptionJourneyRecord] shouldBe sjr
    }
  }
}
