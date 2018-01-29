package uk.gov.hmrc.agentmappingfrontend.support

import uk.gov.hmrc.domain.SaAgentReference

case class SampleUser(authoriseJsonResponse: String, saAgentReference: Option[SaAgentReference] = None)

object SampleUsers {

  val anSAEnrolledAgent = SampleUser(
    s"""{
       |  "authorisedEnrolments": [
       |   { "key":"IR-SA-AGENT", "identifiers": [
       |      { "key":"IRAgentReference", "value": "HZ1234" }
       |    ]}
       |  ],
       |  "affinityGroup": "Agent"
       |}""".stripMargin,
    saAgentReference = Some(SaAgentReference("HZ1234"))
  )

  val anSAEnrolledAgentInactive = SampleUser(
    s"""{
       | "authorisedEnrolments": [
       |  { "key":"IR-SA-AGENT", "identifiers": [
       |     { "key":"IRAgentReference", "value":"HZ1234", "state":"Inactive"}
       |   ]}
       |  ],
       |  "affinityGroup": "Agent"
       | }""".stripMargin,
    saAgentReference = Some(SaAgentReference("HZ1234"))
  )

  val anAgentNotEnrolled = SampleUser(
    s"""{
       | "authorisedEnrolments": [],
       | "affinityGroup": "Agent"
       |}""".stripMargin,
    saAgentReference = Some(SaAgentReference("HZ1234"))
  )

  val individual = SampleUser(
    s"""{
       | "authorisedEnrolments": [
       |  { "key":"IR-SA", "identifiers": [
       |     { "key":"utr", "value": "ABC123456" }
       |   ]}
       | ],
       | "affinityGroup": "Individual"
       |}""".stripMargin
  )
}