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

import uk.gov.hmrc.agentmappingfrontend.model.AuthProviderId
import uk.gov.hmrc.agentmappingfrontend.model.BusinessDetails
import uk.gov.hmrc.agentmappingfrontend.model.BusinessType
import uk.gov.hmrc.agentmappingfrontend.model.Postcode
import uk.gov.hmrc.agentmappingfrontend.model.SubscriptionJourneyRecord
import uk.gov.hmrc.agentmappingfrontend.model.UserMapping
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Utr
import uk.gov.hmrc.domain.AgentCode

trait SubscriptionJourneyRecordSamples {

  val businessDetails = BusinessDetails(
    BusinessType.LimitedCompany,
    Utr("2000000000"),
    Postcode("AA11AA"),
    None,
    None,
    None,
    None
  )

  def sjrBuilder(
    authProviderId: String,
    continueId: Option[String] = None,
    userMappings: List[UserMapping] = List.empty,
    mappingComplete: Boolean = false,
    cleanCredId: Option[AuthProviderId] = None
  ) = SubscriptionJourneyRecord(
    authProviderId = AuthProviderId(authProviderId),
    continueId = continueId,
    businessDetails = businessDetails,
    amlsData = None,
    userMappings = userMappings,
    mappingComplete = mappingComplete,
    cleanCredsAuthProviderId = cleanCredId,
    lastModifiedDate = None
  )

  val sjrNoContinueId = sjrBuilder("12345-credId")

}
