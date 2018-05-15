package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.support.{SampleUser, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  def givenUserIsAuthenticated(user: SampleUser) = {
    user.throwException.fold {
      givenAuthorisedFor("{}", user.authoriseJsonResponse)
    } { e =>
      givenUnauthorisedWith(e.getClass.getSimpleName)
    }
  }

  def givenUserIsNotAuthenticated() = {
    givenUnauthorisedWith("MissingBearerToken")
  }

  def givenAuthorisedFor(serviceName: String): Unit = {
    givenAuthorisedFor(
      "{}",
      s"""{
         |  "authorisedEnrolments": [
         |   { "key":"$serviceName", "identifiers": [
         |      { "key":"foo", "value": "foo" }
         |    ]}
         |  ],
         |  "affinityGroup": "Agent",
         |  "credentials": {
         |    "providerId": "12345-credId",
         |    "providerType": "GovernmentGateway"
         |  }
         |}""".stripMargin
    )
  }

  def givenUnauthorisedWith(mdtpDetail: String): Unit = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")))
  }

  def givenAuthorisationFailsWith5xx(): Unit = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(aResponse()
          .withStatus(500)))
  }

  def givenAuthorisedFor(payload: String, responseBody: String): Unit = {
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
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate",
                        "MDTP detail=\"InsufficientEnrolments\"")))
  }

  def verifyAuthoriseAttempt(): Unit = {
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
  }

}
