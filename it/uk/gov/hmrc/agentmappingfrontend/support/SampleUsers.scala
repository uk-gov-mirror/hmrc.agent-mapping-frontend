package uk.gov.hmrc.agentmappingfrontend.support

import uk.gov.hmrc.agentmappingfrontend.model.Identifier
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.domain.SaAgentReference

case class SampleUser(authoriseJsonResponse: String, identifier: Seq[Identifier] = Seq.empty, throwException: Option[Exception] = None)

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
    identifier = Seq(Identifier("IRAgentReference","HZ1234"))
  )

  val anVATEnrolledAgent = SampleUser(
    s"""{
       |  "authorisedEnrolments": [
       |   { "key":"HMCE-VATDEC-ORG", "identifiers": [
       |      { "key":"VATRegNo", "value": "HZ1234" }
       |    ]}
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    identifier = Seq(Identifier("VATRegNo","HZ1234"))
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
    identifier = Seq(Identifier("IRAgentReference","HZ1234"))
  )

  val anVATEnrolledAgentInactive = SampleUser(
    s"""{
       |  "authorisedEnrolments": [
       |   { "key":"HMCE-VATDEC-ORG",
       |     "identifiers": [
       |        { "key":"VATRegNo", "value": "HZ1234" }
       |      ],
       |     "state": "Inactive" }
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    identifier = Seq(Identifier("VATRegNo","HZ1234"))
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
    identifier = Seq(Identifier("IRAgentReference","HZ1234"))
  )

  val individual = SampleUser("", Seq.empty, Some(InsufficientEnrolments()))
}