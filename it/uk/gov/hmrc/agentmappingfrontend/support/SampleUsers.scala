package uk.gov.hmrc.agentmappingfrontend.support

import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.domain.SaAgentReference

case class SampleUser(authoriseJsonResponse: String, saAgentReference: Option[SaAgentReference] = None, throwException: Option[Exception] = None)

object SampleUsers {

  val anSAEnrolledAgent = SampleUser(
    s"""{
       |  "authorisedEnrolments": [
       |   { "key":"IR-SA-AGENT", "identifiers": [
       |      { "key":"IRAgentReference", "value": "HZ1234" }
       |    ]}
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    saAgentReference = Some(SaAgentReference("HZ1234"))
  )

  val anSAEnrolledAgentInactive = SampleUser(
    s"""{
       |  "authorisedEnrolments": [
       |   { "key":"IR-SA-AGENT",
       |     "identifiers": [
       |        { "key":"IRAgentReference", "value": "HZ1234" }
       |      ],
       |     "state": "Inactive" }
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    saAgentReference = Some(SaAgentReference("HZ1234"))
  )

  val anAgentNotEnrolled = SampleUser(
    s"""{
       | "authorisedEnrolments": [],
       | "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    saAgentReference = Some(SaAgentReference("HZ1234"))
  )

  val individual = SampleUser("", None, Some(InsufficientEnrolments()))
}