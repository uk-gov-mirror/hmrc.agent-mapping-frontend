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
      case i: Invalid                   => Invalid(ValidationError("error.utr.blank"))
      case _ if !isUtrValid(fieldValue) => Invalid(ValidationError("error.utr.invalid"))
      case _                            => Valid
    }
  }

  private val arnConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty(fieldValue) match {
      case i: Invalid                   => Invalid(ValidationError("error.arn.blank"))
      case _ if !isArnValid(fieldValue) => Invalid(ValidationError("error.arn.invalid"))
      case _                            => Valid
    }
  }

  def utr: Mapping[String] = text verifying utrConstraint

  def arn: Mapping[String] = text verifying arnConstraint

  private def isArnValid(arnStr: String): Boolean = normalizeArn(arnStr).nonEmpty

  private def isUtrValid(utrStr: String): Boolean = normalizeUtr(utrStr).nonEmpty

  def normalizeArn(arnStr: String): Option[Arn] = {
    val hyphenPattern = """([A-Z]ARN-\d{3}-\d{4})""".r
    val defaultPattern = """([A-Z]ARN\d{7})""".r

    val formattedArn = arnStr.trim match {
      case hyphenPattern(value)  => Some(value.replace("-", ""))
      case defaultPattern(value) => Some(value)
      case _                     => None
    }

    formattedArn.flatMap(arn => if (Arn.isValid(arn)) Some(Arn(arn)) else None)
  }

  def normalizeUtr(utrStr: String): Option[Utr] = {
    val formattedUtr = utrStr.replace(" ", "")
    if (Utr.isValid(formattedUtr)) Some(Utr(formattedUtr)) else None
  }

  def prettify(arn: Arn): String = {
    val unapplyPattern = """([A-Z]ARN)(\d{3})(\d{4})""".r

    unapplyPattern
      .unapplySeq(arn.value)
      .map(_.mkString("-"))
      .getOrElse(throw new Exception(s"The arn contains an invalid value ${arn.value}"))
  }

  def prettify(utr: Utr): String =
    if (utr.value.trim.length == 10) {
      val (first, last) = utr.value.trim.splitAt(5)
      s"$first $last"
    } else {
      throw new Exception(s"The utr contains an invalid value ${utr.value}")
    }
}
