package uk.gov.hmrc.agentmappingfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentmappingfrontend.model.{AuthProviderId, SubscriptionJourneyRecord}
import uk.gov.hmrc.agentmappingfrontend.support.WireMockSupport
import uk.gov.hmrc.play.encoding.UriPathEncoding.encodePathSegment

trait AgentSubscriptionStubs {

  me: WireMockSupport =>

  def givenNoSubscriptionJourneyRecordFoundForAuthProviderId(authProviderId: AuthProviderId): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-subscription/subscription/journey/id/${encodePathSegment(authProviderId.id)}"))
        .willReturn(aResponse()
          .withStatus(Status.NO_CONTENT)))

  def givenNoSubscriptionJourneyRecordFoundForContinueId(continueId: String): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-subscription/subscription/journey/continueId/${encodePathSegment(continueId)}"))
        .willReturn(aResponse()
          .withStatus(Status.NO_CONTENT)))

  def givenSubscriptionJourneyRecordExistsForAuthProviderId(authProviderId: AuthProviderId, subscriptionJourneyRecord: SubscriptionJourneyRecord): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-subscription/subscription/journey/id/${encodePathSegment(authProviderId.id)}"))
        .willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(subscriptionJourneyRecord).toString()))
    )

  def givenSubscriptionJourneyRecordNotFoundForAuthProviderId(authProviderId: AuthProviderId): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-subscription/subscription/journey/id/${encodePathSegment(authProviderId.id)}"))
        .willReturn(aResponse()
          .withStatus(Status.NO_CONTENT)

    ))

  def givenSubscriptionJourneyRecordExistsForContinueId(continueId: String, subscriptionJourneyRecord: SubscriptionJourneyRecord): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-subscription/subscription/journey/continueId/${encodePathSegment(continueId)}"))
        .willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(subscriptionJourneyRecord).toString()))
    )

  def givenSubscriptionJourneyRecordNotFoundForContinueId(continueId: String): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/agent-subscription/subscription/journey/continueId/${encodePathSegment(continueId)}"))
        .willReturn(aResponse()
          .withStatus(Status.NO_CONTENT))
    )

  def givenUpdateSubscriptionJourneyRecordSucceeds(subscriptionJourneyRecord: SubscriptionJourneyRecord): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/agent-subscription/subscription/journey/primaryId/${encodePathSegment(subscriptionJourneyRecord.authProviderId.id)}"))
        .withRequestBody(equalToJson(
          Json.toJson(subscriptionJourneyRecord).toString(), true, true ))
        .willReturn(aResponse().withStatus(Status.NO_CONTENT))
    )


  def givenUpdateSubscriptionJourneyRecordFails(subscriptionJourneyRecord: SubscriptionJourneyRecord): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/agent-subscription/subscription/journey/primaryId/${encodePathSegment(subscriptionJourneyRecord.authProviderId.id)}"))
        .willReturn(aResponse()
        .withStatus(Status.INTERNAL_SERVER_ERROR))
    )



}
