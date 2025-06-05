/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.util

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play._
import play.api.test._
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentmappingfrontend.util.RequestSupport
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec

class RequestSupportISpec
extends BaseControllerISpec {

  "RequestSupport.hc" should {
    "produce a HeaderCarrier from a RequestHeader" in {
      implicit val rh: RequestHeader = FakeRequest("GET", "/test-path")
      val hc: HeaderCarrier = RequestSupport.hc
      hc mustBe a[HeaderCarrier]
    }
    "have an implicit header carrier in scope when using RequestSupport" in {
      implicit val request: Request[AnyContent] = FakeRequest("GET", "/test-path")
      val requestSupport = new RequestSupport()
      val hc: HeaderCarrier = requestSupport.hc
      hc mustBe a[HeaderCarrier]
    }
  }
}
