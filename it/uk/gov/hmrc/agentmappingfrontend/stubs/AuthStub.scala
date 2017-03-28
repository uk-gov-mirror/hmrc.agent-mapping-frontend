package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmappingfrontend.support.{SampleUser, SessionKeysForTesting}
import uk.gov.hmrc.play.http.SessionKeys

object AuthStub {
  def userIsNotAuthenticated(): Unit = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(401)))
  }

  def userIsAuthenticated(user: SampleUser): Seq[(String, String)] = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(200).withBody(user.authJson)))
    stubFor(get(urlMatching("/auth/oid/[^/]+$")).willReturn(aResponse().withStatus(200).withBody(user.authJson)))
    stubFor(get(urlEqualTo(user.userDetailsLink)).willReturn(aResponse().withStatus(200).withBody(user.userDetailsJson)))

    sessionKeysForMockAuth(user)
  }

  private def sessionKeysForMockAuth(user: SampleUser): Seq[(String, String)] = Seq(
    SessionKeys.userId -> user.authorityUri,
    SessionKeysForTesting.token -> "fakeToken")

  def isNotEnrolled(user: SampleUser): Unit = {
    stubFor(get(urlEqualTo(user.enrolmentsLink)).willReturn(aResponse().withStatus(200).withBody("[]")))
  }

  def isEnrolled(user: SampleUser): Unit = {
    stubFor(get(urlEqualTo(user.enrolmentsLink)).willReturn(aResponse().withStatus(200).withBody(
      s"""|[{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IrAgentReference","value":"HZ1234"}],"state":"Activated"}]""".stripMargin)))
  }
}
