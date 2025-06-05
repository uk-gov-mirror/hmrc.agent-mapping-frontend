/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.Inject
import javax.inject.Singleton
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.helper.urlEncode

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig {

  val appName: String = "agent-mapping-frontend"

  val agentServicesFrontendBaseUrl: String
  val companyAuthFrontendBaseUrl: String
  val agentMappingBaseUrl: String
  val agentSubscriptionBaseUrl: String
  val agentSubscriptionFrontendBaseUrl: String
  val agentMappingFrontendBaseUrl: String
  val agentClientAuthorisationBaseUrl: String
  val contactFrontendHost: String
  val clientCountMaxRecords: Int
  val timeout: Int
  val timeoutCountdown: Int
  val languageToggle: Boolean

  // derived values
  private val contactFormServiceIdentifier = "AOSS"
  lazy val reportAProblemPartialUrl = s"$contactFrontendHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactFrontendHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val signOutRedirectUrl: String = s"$agentMappingFrontendBaseUrl/agent-mapping/start-submit"
  lazy val taskListSignOutRedirectUrl: String = s"$agentMappingFrontendBaseUrl/agent-mapping/task-list/start-submit"
  lazy val agentSubscriptionFrontendTaskListUrl: String = s"$agentSubscriptionFrontendBaseUrl/task-list"
  lazy val agentSubscriptionFrontendProgressSavedUrl = s"$agentSubscriptionFrontendBaseUrl/progress-saved/?backLink=$agentMappingFrontendBaseUrl/agent-mapping"
  lazy val signInAndContinue = s"$companyAuthFrontendBaseUrl/gg/sign-in?continue=${urlEncode(agentServicesFrontendBaseUrl)}"

  val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

}
@Singleton
class FrontendAppConfig @Inject() (servicesConfig: ServicesConfig)
extends AppConfig {

  override lazy val contactFrontendHost: String = servicesConfig.getString("contact-frontend.host")

  // base urls
  override lazy val companyAuthFrontendBaseUrl: String = servicesConfig.getString("microservice.services.company-auth-frontend.external-url")
  override lazy val agentSubscriptionBaseUrl: String = servicesConfig.baseUrl("agent-subscription")
  override lazy val agentMappingBaseUrl: String = servicesConfig.baseUrl("agent-mapping")
  override lazy val agentSubscriptionFrontendBaseUrl: String =
    s"${servicesConfig.getString("microservice.services.agent-subscription-frontend.external-url")}/agent-subscription"
  override lazy val agentMappingFrontendBaseUrl: String = s"${servicesConfig.getString("microservice.services.agent-mapping-frontend.external-url")}"
  override lazy val agentServicesFrontendBaseUrl: String =
    s"${servicesConfig.getString("microservice.services.agent-services-account-frontend.external-url")}/agent-services-account"
  override val agentClientAuthorisationBaseUrl: String = servicesConfig.baseUrl("agent-client-authorisation")

  override lazy val clientCountMaxRecords: Int = servicesConfig.getInt("clientCount.maxRecords")

  override val timeout: Int = servicesConfig.getInt("timeoutDialog.timeout-seconds")
  override val timeoutCountdown: Int = servicesConfig.getInt("timeoutDialog.timeout-countdown-seconds")

  override val languageToggle: Boolean = servicesConfig.getBoolean("features.enable-welsh-toggle")

}
