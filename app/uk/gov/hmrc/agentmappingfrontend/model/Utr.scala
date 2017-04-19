package uk.gov.hmrc.agentmappingfrontend.model

import uk.gov.hmrc.agentmappingfrontend.controllers.CheckUTR

object Utr {

  private val utrPattern = "^\\d{10}$".r

  def isValid(utr: String): Boolean =
    utr match {
      case utrPattern(_*) => CheckUTR.isValidUTR(utr)
      case _ => false
    }
}
