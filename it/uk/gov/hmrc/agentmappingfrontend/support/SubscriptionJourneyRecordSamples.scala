package uk.gov.hmrc.agentmappingfrontend.support

import uk.gov.hmrc.agentmappingfrontend.model.{AuthProviderId, BusinessDetails, BusinessType, Postcode, SubscriptionJourneyRecord, UserMapping}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.AgentCode

trait SubscriptionJourneyRecordSamples {

  val businessDetails = BusinessDetails(BusinessType.LimitedCompany,Utr("2000000000"),Postcode("AA11AA"),None,None,None,None)

  def sjrBuilder(
                  authProviderId: String,
                  continueId: Option[String] = None,
                  userMappings: List[UserMapping] = List.empty,
                  mappingComplete: Boolean = false, cleanCredId: Option[AuthProviderId] = None) = SubscriptionJourneyRecord(
    authProviderId = AuthProviderId(authProviderId),
    continueId = continueId,
    businessDetails = businessDetails,
    amlsData = None,
    userMappings = userMappings,
    mappingComplete = mappingComplete,
    cleanCredsAuthProviderId = cleanCredId,
    lastModifiedDate = None)

  val sjrNoContinueId = sjrBuilder("12345-credId")
  val sjrWithNoUserMappings = sjrNoContinueId.copy(continueId = Some("continue-id"))
  val sjrWithCleanCredId = sjrWithNoUserMappings.copy(cleanCredsAuthProviderId = Some(AuthProviderId("12345-credId")))
  val sjrWithMapping = sjrWithNoUserMappings.copy(userMappings =
    List(
      UserMapping(
        authProviderId = AuthProviderId("1-credId"),
        agentCode = Some(AgentCode("agentCode-1")),
        count = 1,
        legacyEnrolments = List.empty,
        ggTag = "6666")))

  val sjrWithUserAlreadyMapped = sjrWithNoUserMappings.copy(userMappings =
    List(
      UserMapping(
        authProviderId = AuthProviderId("12345-credId"),
        agentCode = Some(AgentCode("agentCode-1")),
        count = 1,
        legacyEnrolments = List.empty,
        ggTag = "6666")))
}
