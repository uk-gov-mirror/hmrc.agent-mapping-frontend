package uk.gov.hmrc.agentmappingfrontend.support

object SessionKeysForTesting {
  // Workaround to hide the deprecation warning. It it OK to use this here, as long as we write sessions and not read them.
  val token = "token" // SessionKeys.token
}
