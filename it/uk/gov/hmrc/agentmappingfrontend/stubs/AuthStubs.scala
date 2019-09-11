package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentmappingfrontend.support.{SampleUser, WireMockSupport}

trait AuthStubs {
  me: WireMockSupport =>

  def givenUserIsAuthenticated(user: SampleUser): StubMapping =
    user.throwException.fold {
      givenAuthorisedFor("{}", user.authoriseJsonResponse)
    } { e =>
      givenUnauthorisedWith(e.getClass.getSimpleName)
    }

  def givenUserIsNotAuthenticated(): StubMapping =
    givenUnauthorisedWith("MissingBearerToken")

  def givenAuthorisedFor(enrolmentKey: String): StubMapping =
    givenAuthorisedFor(
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

  def givenUnauthorisedWith(mdtpDetail: String): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")))

  def givenAuthorisationFailsWith5xx(): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(aResponse()
          .withStatus(500)))

  def givenAuthorisedFor(payload: String, responseBody: String): StubMapping = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)))

    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(aResponse()
          .withStatus(401)
          .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))
  }

  def givenuserHasUnsupportedAffinityGroup(): StubMapping = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAffinityGroup\"")))
  }

  def givenUserHasUnsupportedAuthProvider(): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAuthProvider\"")))

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

}
