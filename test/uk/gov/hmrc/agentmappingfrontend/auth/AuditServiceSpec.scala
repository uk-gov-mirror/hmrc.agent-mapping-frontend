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

package uk.gov.hmrc.agentmappingfrontend.auth

import uk.gov.hmrc.agentmappingfrontend.audit.AuditService
import uk.gov.hmrc.play.test.UnitSpec

class AuditServiceSpec extends UnitSpec {
  "AuditService" should {
    "convert audit type to transaction name" in {
      AuditService.toTransactionName("") shouldBe ""
      AuditService.toTransactionName("text") shouldBe "text"
      AuditService.toTransactionName("TEXT") shouldBe "text"
      AuditService.toTransactionName("SomeTEXT") shouldBe "some-text"
      AuditService.toTransactionName("SomeCAMELCaseText") shouldBe "some-camel-case-text"
      AuditService.toTransactionName("SomeCAMELCaseTEXT") shouldBe "some-camel-case-text"
      AuditService.toTransactionName("SOMECamelCASEText") shouldBe "some-camel-case-text"
      AuditService.toTransactionName("SomeCamelCaseText") shouldBe "some-camel-case-text"
      AuditService.toTransactionName("someCamelCaseText") shouldBe "some-camel-case-text"
    }
  }
}
