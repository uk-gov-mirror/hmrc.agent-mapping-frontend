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
import sttp.model.Uri.UriContext
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig:

  val appName: String = "agent-mapping-frontend"

  lazy val agentServicesFrontendBaseUrl: String
  lazy val basGatewayFrontendExternalUrl: String
  lazy val agentMappingBaseUrl: String
  lazy val agentMappingFrontendBaseUrl: String
  lazy val signOutPath: String
  lazy val signInPath: String
  val timeout: Int
  val timeoutCountdown: Int

  // derived values
  lazy val signOutUrl: String = s"$basGatewayFrontendExternalUrl$signOutPath"
  lazy val signInUrl: String = s"$basGatewayFrontendExternalUrl$signInPath"
  lazy val signOutRedirectUrl: String = s"$agentMappingFrontendBaseUrl/agent-mapping/start-submit"
  lazy val signInAndContinue: String = uri"$signInUrl?continue_url=$agentServicesFrontendBaseUrl".toString

  val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

@Singleton
class FrontendAppConfig @Inject() (servicesConfig: ServicesConfig)
extends AppConfig:

  // base urls
  override lazy val basGatewayFrontendExternalUrl: String = servicesConfig.getString("microservice.services.bas-gateway-frontend.external-url")
  override lazy val agentMappingBaseUrl: String = servicesConfig.baseUrl("agent-mapping")
  override lazy val agentMappingFrontendBaseUrl: String = s"${servicesConfig.getString("microservice.services.agent-mapping-frontend.external-url")}"
  override lazy val agentServicesFrontendBaseUrl: String =
    s"${servicesConfig.getString("microservice.services.agent-services-account-frontend.external-url")}/agent-services-account"

  override lazy val signOutPath: String = servicesConfig.getString("microservice.services.bas-gateway-frontend.sign-out.path")
  override lazy val signInPath: String = servicesConfig.getString("microservice.services.bas-gateway-frontend.sign-in.path")

  override val timeout: Int = servicesConfig.getInt("timeoutDialog.timeout-seconds")
  override val timeoutCountdown: Int = servicesConfig.getInt("timeoutDialog.timeout-countdown-seconds")
