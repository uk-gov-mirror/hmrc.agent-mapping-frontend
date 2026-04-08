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

case class SampleUser(
  authoriseJsonResponse: String,
  activeEnrolments: Set[String],
  throwException: Option[Exception] = None
)

object SampleUsers:

  val saAgentCode = "HZ1234"
  val eligibleAgent = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"IR-SA-AGENT", "identifiers": [
       |      { "key":"IRAgentReference", "value": "$saAgentCode" }
       |    ]}
       |  ],
       |  "optionalCredentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "affinityGroup": "Agent"
       |}""".stripMargin,
    activeEnrolments = Set("IR-SA-AGENT")
  )

  val saEnrolledAgentInactive = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"IR-SA-AGENT",
       |     "identifiers": [
       |        { "key":"IRAgentReference", "value": "HZ1234" }
       |      ],
       |     "state": "Inactive" }
       |  ],
       |  "optionalCredentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "affinityGroup": "Agent"
       |}""".stripMargin,
    activeEnrolments = Set()
  )

  val agentNotEnrolled = SampleUser(
    s"""{
       | "allEnrolments": [],
       |  "optionalCredentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "affinityGroup": "Agent"
       |}""".stripMargin,
    activeEnrolments = Set()
  )

  val mtdAsAgent = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"HMRC-AS-AGENT", "identifiers": [
       |      { "key":"AgentReferenceNumber", "value": "TARN0000001" }
       |    ]}
       |  ],
       |  "optionalCredentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "affinityGroup": "Agent"
       |}""".stripMargin,
    activeEnrolments = Set("HMRC-AS-AGENT")
  )

  val notEligibleAgent = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"FOO", "identifiers": [
       |      { "key":"fooIdentifier", "value": "foo123" }
       |    ]}
       |  ],
       |  "optionalCredentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "affinityGroup": "Agent"
       |}""".stripMargin,
    activeEnrolments = Set("FOO")
  )

  val individual = SampleUser(
    s"""{
       | "allEnrolments": [],
       |  "optionalCredentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "affinityGroup": "Individual"
       |}""".stripMargin,
    activeEnrolments = Set()
  )
