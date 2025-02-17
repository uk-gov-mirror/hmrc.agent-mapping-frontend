/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.http.BadRequestException

sealed trait RadioInputAnswer extends Product with Serializable

object RadioInputAnswer {
  case object Yes extends RadioInputAnswer
  case object No extends RadioInputAnswer

  def apply(str: String): RadioInputAnswer = str.toLowerCase match {
    case "yes" => Yes
    case "no"  => No
    case _     => throw new BadRequestException("Strange form input value")
  }

  def unapply(answer: RadioInputAnswer): Option[String] =
    answer match {
      case Yes => Some("yes")
      case No  => Some("no")
    }
}
