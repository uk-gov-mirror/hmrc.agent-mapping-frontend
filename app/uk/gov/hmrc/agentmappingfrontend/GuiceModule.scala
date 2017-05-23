/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend

import java.net.URL
import javax.inject.Provider

import com.google.inject.AbstractModule
import com.google.inject.name.Names.named
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentmappingfrontend.config._
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HttpGet, HttpPut}

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule with ServicesConfig {
  override protected lazy val mode: Mode = environment.mode
  override protected lazy val runModeConfiguration: Configuration = configuration

  override def configure(): Unit = {
    bind(classOf[HttpGet]).toInstance(WSHttp)
    bind(classOf[HttpPut]).toInstance(WSHttp)
    bind(classOf[AppConfig]).to(classOf[FrontendAppConfig])
    bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[AuditConnector]).toInstance(new MicroserviceAuditConnector)
    bindBaseUrl("agent-mapping")
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private class ConfigPropertyProvider(propertyName: String) extends Provider[String] {
    override lazy val get = getConfString(propertyName, throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }
}

class MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

