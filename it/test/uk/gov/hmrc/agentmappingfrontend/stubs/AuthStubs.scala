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

package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentmappingfrontend.support.SampleUser
import uk.gov.hmrc.agentmappingfrontend.support.WireMockSupport

trait AuthStubs {
  me: WireMockSupport =>

  def givenUserIsAuthenticated(user: SampleUser): StubMapping =
    user.throwException.fold {
      givenAuthorisedFor("{}", user.authoriseJsonResponse)
    } { e =>
      givenUnauthorisedWith(e.getClass.getSimpleName)
    }

  def givenUserIsNotAuthenticated(): StubMapping = givenUnauthorisedWith("MissingBearerToken")

  def givenAuthorisedFor(enrolmentKey: String): StubMapping = givenAuthorisedFor(
    "{}",
    s"""{
       |  "allEnrolments": [
       |   { "key":"$enrolmentKey", "identifiers": [
       |      { "key":"foo", "value": "foo" }
       |    ]}
       |  ],
       |  "optionalCredentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin
  )

  def givenUnauthorisedWith(mdtpDetail: String): StubMapping = stubFor(
    post(urlEqualTo("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(401)
          .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")
      )
  )

  def givenAuthorisationFailsWith5xx(): StubMapping = stubFor(
    post(urlEqualTo("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(500)
      )
  )

  def givenAuthorisedFor(
    payload: String,
    responseBody: String
  ): StubMapping = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(
          payload,
          true,
          true
        ))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
        )
    )
  }

  def givenuserHasUnsupportedAffinityGroup(): StubMapping = stubFor(
    post(urlEqualTo("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(401)
          .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAffinityGroup\"")
      )
  )

  def givenUserHasUnsupportedAuthProvider(): StubMapping = stubFor(
    post(urlEqualTo("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(401)
          .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAuthProvider\"")
      )
  )

  def verifyAuthoriseAttempt(): Unit = verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

}
