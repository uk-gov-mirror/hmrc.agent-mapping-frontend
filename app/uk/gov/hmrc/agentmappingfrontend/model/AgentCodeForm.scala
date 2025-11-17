/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError

object AgentCodeForm {

  val fieldName = "agentCode"
  val regex = "[a-zA-Z0-9]*"
  val length = 6

  val form: Form[String] = {
    Form(
      single(
        fieldName -> text
          .transform(_.trim, identity[String])
          .verifying(validateAgentCode())
      )
    )
  }

  private def validateAgentCode(): Constraint[String] = {
    Constraint[String] { agentCode: String =>
      agentCode match {
        case value if value.isEmpty => Invalid(ValidationError("agentCode.error.required"))
        case value if value.length != length && !value.matches(regex) => Invalid(ValidationError("agentCode.error.lengthAndFormat"))
        case value if value.length != length => Invalid(ValidationError("agentCode.error.length"))
        case value if !value.matches(regex) => Invalid(ValidationError("agentCode.error.format"))
        case _ => Valid
      }
    }
  }

}
