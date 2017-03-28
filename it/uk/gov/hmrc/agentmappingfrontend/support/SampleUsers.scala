package uk.gov.hmrc.agentmappingfrontend.support

import play.api.libs.json.JsValue
import play.api.libs.json.Json.parse

case class SampleUser(authJson: String, userDetailsJson: String) {
  private val json: JsValue = parse(authJson)

  val authorityUri: String = (json \ "uri").as[String]
  val userDetailsLink: String = (json \ "userDetailsLink").as[String]
  val enrolmentsLink: String = (json \ "enrolments").as[String]
}

object SampleUsers {
  private val subscribingAgentOid = "1234567890"
  private val subscribingAgentUserDetailsLink: String = s"/user-details/id/$subscribingAgentOid"
  def subscribingAgent(implicit wireMockBaseUrl: WireMockBaseUrl) = SampleUser(
    s"""
       |{
       |  "uri": "/auth/oid/$subscribingAgentOid",
       |  "userDetailsLink": "$subscribingAgentUserDetailsLink",
       |  "loggedInAt": "2015-01-19T11:11:34.926Z",
       |  "credentials": {
       |    "gatewayId": "cred-id-12345",
       |    "idaPids": []
       |  },
       |  "accounts": {
       |    "version": 1
       |  },
       |  "lastUpdated": "2015-01-19T11:11:34.926Z",
       |  "credentialStrength": "weak",
       |  "confidenceLevel": 50,
       |  "enrolments": "/auth/oid/$subscribingAgentOid/enrolments",
       |  "legacyOid": "$subscribingAgentOid"
       |}
    """.stripMargin,
    userDetailsJson = s"""
                         |{
                         |  "affinityGroup": "Agent"
                         |}
    """.stripMargin
  )

  private val individualOid = "234567891"
  private val individualUserDetailsLink: String = s"/user-details/id/$individualOid"

  def individual(implicit wireMockBaseUrl: WireMockBaseUrl) = SampleUser(
    s"""
       |{
       |  "uri": "/auth/oid/$individualOid",
       |  "userDetailsLink": "$individualUserDetailsLink",
       |  "loggedInAt": "2015-01-19T11:11:34.926Z",
       |  "credentials": {
       |    "gatewayId": "cred-id-12345",
       |    "idaPids": []
       |  },
       |  "accounts": {
       |    "version": 1
       |  },
       |  "lastUpdated": "2015-01-19T11:11:34.926Z",
       |  "credentialStrength": "weak",
       |  "confidenceLevel": 50,
       |  "enrolments": "/auth/oid/$individualOid/enrolments",
       |  "legacyOid": "$individualOid"
       |}
    """.stripMargin,
    userDetailsJson = s"""
                         |{
                         |  "affinityGroup": "Individual"
                         |}
    """.stripMargin

  )
}