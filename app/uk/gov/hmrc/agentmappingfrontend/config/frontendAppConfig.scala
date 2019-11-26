/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.config

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.helper.urlEncode

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  def accessibilityUrl(userAction: String): String
  val signOutRedirectUrl: String
  val taskListSignOutRedirectUrl: String
  val signInAndContinue: String
  val agentServicesFrontendBaseUrl: String
  val companyAuthFrontendBaseUrl: String
  val agentMappingBaseUrl: String
  val agentSubscriptionBaseUrl: String
  val agentSubscriptionFrontendBaseUrl: String
  val agentSubscriptionFrontendTaskListUrl: String
  val agentSubscriptionFrontendProgressSavedUrl: String
  val agentMappingFrontendBaseUrl: String
  val appName: String
  val clientCountMaxRecords: Int
  val timeout: Int
  val timeoutCountdown: Int
}
@Singleton
class FrontendAppConfig @Inject()(servicesConfig: ServicesConfig) extends AppConfig {

  override val appName = "agent-mapping-frontend"

  def getConf(key: String): String = servicesConfig.getString(key)

  private val contactFormServiceIdentifier = "AOSS"

  private lazy val contactFrontendHost: String = servicesConfig.getString("contact-frontend.host")

  override lazy val analyticsToken: String = servicesConfig.getString(s"google-analytics.token")
  override lazy val analyticsHost: String = servicesConfig.getString(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl =
    s"$contactFrontendHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl =
    s"$contactFrontendHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override def accessibilityUrl(userAction: String): String =
    s"$contactFrontendHost/contact/accessibility?service=$contactFormServiceIdentifier&userAction=$userAction"

  //base urls
  override lazy val companyAuthFrontendBaseUrl: String =
    servicesConfig.getString("microservice.services.company-auth-frontend.external-url")
  override lazy val agentSubscriptionBaseUrl: String =
    servicesConfig.baseUrl("agent-subscription")
  override lazy val agentMappingBaseUrl: String = servicesConfig.baseUrl("agent-mapping")
  override lazy val agentSubscriptionFrontendBaseUrl: String =
    s"${servicesConfig.getString("microservice.services.agent-subscription-frontend.external-url")}/agent-subscription"
  override lazy val agentMappingFrontendBaseUrl: String =
    s"${servicesConfig.getString("microservice.services.agent-mapping-frontend.external-url")}"
  override lazy val agentServicesFrontendBaseUrl: String =
    s"${servicesConfig.getString("microservice.services.agent-services-account-frontend.external-url")}/agent-services-account"

  //constructed urls
  override lazy val signOutRedirectUrl: String = s"$agentMappingFrontendBaseUrl/agent-mapping/start-submit"
  override lazy val taskListSignOutRedirectUrl: String =
    s"$agentMappingFrontendBaseUrl/agent-mapping/task-list/start-submit"
  override val agentSubscriptionFrontendTaskListUrl: String =
    s"$agentSubscriptionFrontendBaseUrl/task-list"
  override lazy val agentSubscriptionFrontendProgressSavedUrl =
    s"$agentSubscriptionFrontendBaseUrl/progress-saved/?backLink=$agentMappingFrontendBaseUrl/agent-mapping"
  override lazy val signInAndContinue =
    s"$companyAuthFrontendBaseUrl/gg/sign-in?continue=${urlEncode(agentServicesFrontendBaseUrl)}"

  override lazy val clientCountMaxRecords: Int = servicesConfig.getInt("clientCount.maxRecords")

  override val timeout: Int = servicesConfig.getInt("timeoutDialog.timeout-seconds")
  override val timeoutCountdown: Int = servicesConfig.getInt("timeoutDialog.timeout-countdown-seconds")
}
