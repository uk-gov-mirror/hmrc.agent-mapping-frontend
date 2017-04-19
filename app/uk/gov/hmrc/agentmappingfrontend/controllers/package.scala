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

import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation._
import uk.gov.hmrc.agentmappingfrontend.model.Arn
import uk.gov.hmrc.domain.{Modulus11Check, Modulus23Check}

package object controllers {
  private val utrPatternConstraint = Constraints.pattern("^\\d{10}$".r, error = "error.utr.invalid")

  private val utrConstraint: Constraint[String] = Constraint[String] {
    fieldValue: String =>
      Constraints.nonEmpty(fieldValue) match {
        case i: Invalid => i
        case Valid =>
          utrPatternConstraint(fieldValue) match {
            case i: Invalid => i
            case Valid => if (CheckUTR.isValidUTR(fieldValue)) Valid else Invalid(ValidationError("error.utr.invalid"))
          }
      }
  }

  private val modulus23ArnConstraint: Constraint[String] = Constraint[String] {
    fieldValue: String =>
      Constraints.nonEmpty(fieldValue) match {
        case i: Invalid => i
        case _ if ! Arn.isValid(fieldValue) => Invalid(ValidationError("error.arn.invalid"))
        case _ => Valid
      }
  }

  object CheckUTR extends Modulus11Check {
    def isValidUTR(utr: String): Boolean = {
      val suffix: String = utr.substring(1)
      val checkCharacter: Char = calculateCheckCharacter(suffix)
      checkCharacter == utr.charAt(0)
    }
  }

  object CheckArn extends Modulus23Check {
    def isValidArn(arn: String): Boolean = {
      val suffix: String = arn.substring(1)
      val checkCharacter: Char = calculateCheckCharacter(suffix)
      checkCharacter == arn.charAt(0)
    }
  }

  def utr: Mapping[String] = text verifying utrConstraint

  def arn: Mapping[String] = text verifying modulus23ArnConstraint
}
