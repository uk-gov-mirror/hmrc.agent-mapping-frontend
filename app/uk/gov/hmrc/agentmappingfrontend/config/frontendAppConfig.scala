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
  val signOutRedirectUrl: String
  val taskListSignOutRedirectUrl: String
  val signInAndContinue: String
  val authenticationLoginCallbackUrl: String
  val agentServicesFrontendExternalUrl: String
  val companyAuthFrontendExternalUrl: String
  val agentMappingBaseUrl: String
  val agentSubscriptionBaseUrl: String
  val agentSubscriptionFrontendExternalUrl: String
  val agentSubscriptionFrontendTaskListPath: String
  val agentSubscriptionFrontendReturnAfterMappingPath: String
  val agentSubscriptionFrontendProgressSavedPath: String
  val agentSubscriptionFrontendProgressSavedUrl: String
  val agentMappingFrontendExternalUrl: String
  val ggSignIn: String
  val appName: String
  val clientCountMaxRecords: Int
}
@Singleton
class FrontendAppConfig @Inject()(servicesConfig: ServicesConfig) extends AppConfig {

  override val appName = "agent-mapping-frontend"

  def getConf(key: String) = servicesConfig.getString(key)

  private val contactFormServiceIdentifier = "AOSS"

  private lazy val contactFrontendHost: String = servicesConfig.getString("contact-frontend.host")

  private lazy val startMappingAfterLoggin: String =
    servicesConfig.getString("microservice.services.company-auth-frontend.sign-in.continue-url")

  override lazy val analyticsToken: String = servicesConfig.getString(s"google-analytics.token")
  override lazy val analyticsHost: String = servicesConfig.getString(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl =
    s"$contactFrontendHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl =
    s"$contactFrontendHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  override lazy val companyAuthFrontendExternalUrl =
    servicesConfig.getString("microservice.services.company-auth-frontend.external-url")

  override lazy val agentSubscriptionBaseUrl = servicesConfig.baseUrl("agent-subscription")

  override lazy val agentMappingBaseUrl = servicesConfig.baseUrl("agent-mapping")

  override lazy val ggSignIn = servicesConfig.getString("microservice.services.company-auth-frontend.sign-in.path")
  override lazy val signOutRedirectUrl =
    servicesConfig.getString("microservice.services.company-auth-frontend.sign-out.redirect-url")
  override lazy val taskListSignOutRedirectUrl =
    servicesConfig.getString("microservice.services.company-auth-frontend.sign-out.taskList.redirect-url")
  override lazy val authenticationLoginCallbackUrl: String =
    servicesConfig.getString("authentication.login-callback.url")
  override lazy val agentServicesFrontendExternalUrl =
    servicesConfig.getString("microservice.services.agent-services-account-frontend.external-url")
  override lazy val agentSubscriptionFrontendExternalUrl =
    servicesConfig.getString("microservice.services.agent-subscription-frontend.external-url")
  override lazy val agentSubscriptionFrontendTaskListPath =
    servicesConfig.getString("microservice.services.agent-subscription-frontend.task-list.path")
  override lazy val agentSubscriptionFrontendReturnAfterMappingPath = servicesConfig.getString(
    "microservice.services.agent-subscription-frontend.return-from-mapping.path"
  )

  override lazy val agentSubscriptionFrontendProgressSavedPath = servicesConfig.getString(
    "microservice.services.agent-subscription-frontend.progress-saved.path"
  )

  override lazy val agentSubscriptionFrontendProgressSavedUrl =
    s"$agentSubscriptionFrontendExternalUrl$agentSubscriptionFrontendProgressSavedPath/?backLink=$agentMappingFrontendExternalUrl"

  override lazy val signInAndContinue =
    s"$companyAuthFrontendExternalUrl$ggSignIn?continue=${urlEncode(startMappingAfterLoggin)}"

  override lazy val agentMappingFrontendExternalUrl = servicesConfig.getString(
    s"microservice.services.agent-mapping-frontend.external-url"
  )

  override lazy val clientCountMaxRecords: Int = servicesConfig.getInt("clientCount.maxRecords")

}
