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

package uk.gov.hmrc.agentmappingfrontend

import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr

package object controllers {

  private val utrConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty(fieldValue) match {
      case i: Invalid                    => i
      case _ if !Utr.isValid(fieldValue) => Invalid(ValidationError("error.utr.invalid"))
      case _                             => Valid
    }
  }

  private val arnConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty(fieldValue) match {
      case i: Invalid                    => i
      case _ if !Arn.isValid(fieldValue) => Invalid(ValidationError("error.arn.invalid"))
      case _                             => Valid
    }
  }

  def utr: Mapping[String] = text verifying utrConstraint

  def arn: Mapping[String] = text verifying arnConstraint
}
