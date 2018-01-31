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

package uk.gov.hmrc.agentmappingfrontend.model

import play.api.libs.json.Format
import play.api.libs.json.Json._

case class Identifier(key: String, value: String, activated: Boolean = true){
  override def toString: String = s"$key~$value"
}

object Identifier {
  implicit val formats: Format[Identifier] = format[Identifier]

  def parse(identifier: String): Identifier = {
    val args = identifier.split("~")
    if(args.size!=2) throw new IllegalArgumentException("Identifier format shall be KEY~VALUE.")
    Identifier(args(0),args(1))
  }
}

case class Mapping(arn: String, saAgentReference: Identifier)

object Mapping {
  implicit val formats: Format[Mapping] = format[Mapping]
}