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

import javax.inject.{Inject, Singleton}

import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig
import views.html.helper.urlEncode

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val signOutRedirectUrl: String
  val signInAndContinue: String
  val authenticationLoginCallbackUrl: String
  val agentServicesFrontendExternalUrl: String
  val companyAuthFrontendExternalUrl: String
  val ggSignIn: String
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

  private lazy val startMappingAfterLoggin: String =
    loadConfig("microservice.services.company-auth-frontend.sign-in.continue-url")

  override lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl =
    s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl =
    s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  override lazy val companyAuthFrontendExternalUrl = loadConfig(
    "microservice.services.company-auth-frontend.external-url")

  override lazy val ggSignIn = loadConfig("microservice.services.company-auth-frontend.sign-in.path")
  override lazy val signOutRedirectUrl = loadConfig("microservice.services.company-auth-frontend.sign-out.redirect-url")
  override lazy val authenticationLoginCallbackUrl: String = loadConfig("authentication.login-callback.url")
  override lazy val agentServicesFrontendExternalUrl = loadConfig(
    "microservice.services.agent-services-account-frontend.external-url")
  override lazy val signInAndContinue =
    s"$companyAuthFrontendExternalUrl$ggSignIn?continue=${urlEncode(startMappingAfterLoggin)}"
}
