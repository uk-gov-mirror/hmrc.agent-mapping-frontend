/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import views.html.helper.urlEncode

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val signOutUrl: String
  val signOutAndRedirectUrl: String
  val authenticationLoginCallbackUrl: String
  val agentServicesFrontendExternalUrl: String

}

trait StrictConfig {
  def configuration: Configuration
  def loadConfig(key: String): String =
    configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))
}

@Singleton
class FrontendAppConfig @Inject()(val environment: Environment, val configuration: Configuration)
    extends AppConfig with ServicesConfig with StrictConfig {

  override val runModeConfiguration: Configuration = configuration
  override protected def mode: Mode = environment.mode

  private lazy val contactHost = runModeConfiguration.getString(s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "AOSS"

  override lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl =
    s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl =
    s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  private lazy val companyAuthFrontendExternalUrl = loadConfig(
    "microservice.services.company-auth-frontend.external-url")
  private lazy val signOutPath = loadConfig("microservice.services.company-auth-frontend.sign-out.path")
  private lazy val signOutContinueUrl = loadConfig("microservice.services.company-auth-frontend.sign-out.continue-url")
  private lazy val signOutRedirectUrl = loadConfig("microservice.services.company-auth-frontend.sign-out.redirect-url")
  override lazy val signOutUrl: String =
    s"$companyAuthFrontendExternalUrl$signOutPath?continue=${urlEncode(signOutContinueUrl)}"
  override lazy val signOutAndRedirectUrl: String =
    s"$companyAuthFrontendExternalUrl$signOutPath?continue=${urlEncode(signOutRedirectUrl)}"
  override lazy val authenticationLoginCallbackUrl: String = loadConfig("authentication.login-callback.url")
  override lazy val agentServicesFrontendExternalUrl = loadConfig(
    "microservice.services.agent-services-account-frontend.external-url")

}
