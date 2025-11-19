/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.support

import org.apache.pekko.stream.Materializer
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers._

trait EndpointBehaviours
extends AuthStubs {
  me: UnitSpec
    with WireMockSupport =>

  type PlayRequest = Request[AnyContent] => Result

  protected def fakeRequest(
    endpointMethod: String,
    endpointPath: String
  ): FakeRequest[AnyContentAsEmpty.type]
  protected def materializer: Materializer

  implicit lazy val mat: Materializer = materializer

  protected def anAuthenticatedEndpoint(
    endpointMethod: String,
    endpointPath: String,
    doRequest: Request[AnyContentAsEmpty.type] => Result
  ): Unit =
    "redirect to the sign-in page if the current user is not logged in" in {
      givenUserIsNotAuthenticated()
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") should include("/bas-gateway/sign-in")
    }

  protected def anEndpointReachableIfSignedInWithEligibleEnrolment(
    endpointMethod: String,
    endpointPath: String
  )(
    doRequest: Request[AnyContentAsEmpty.type] => Result
  ): Unit = {
    behave like anAuthenticatedEndpoint(
      endpointMethod,
      endpointPath,
      doRequest
    )

    "redirect to /problem-with-details page if the current user does not have the IR-SA-AGENT enrolment" in {
      givenUserIsAuthenticated(notEligibleAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.problemWithDetails(id = "someArnRefForMapping").url
    }

    "redirect to /wrong-sign-in-asa page if the current user has an HMRC-AS-AGENT enrolment" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header
        .headers("Location") shouldBe routes.MappingController.wrongSignInDetailsAsa(id = "someArnRefForMapping").url
    }

    "redirect to /wrong-sign-in-not-agent page if the current user is not an agent" in {
      givenUserIsAuthenticated(individual)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header
        .headers("Location") shouldBe routes.MappingController.wrongSignInDetailsNotAgent(id = "someArnRefForMapping").url
    }

    "render the /problem-with-details page if the current user has only inactive enrolments" in {
      givenUserIsAuthenticated(saEnrolledAgentInactive)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.problemWithDetails(id = "someArnRefForMapping").url
    }
  }

}
