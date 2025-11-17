import sbt.Keys.parallelExecution
import sbt.{Setting, Test}
import scoverage.ScoverageKeys

object CoverageSettings {
  private val excludedPackages = List(
    "<empty>",
    ".*Routes.*",
  ).mkString(";")


  private val excludedFiles = Seq(
    ".*template",
    ".*UriPathEncoding",
    ".*SimpleObjectBinder",
    ".*UrlBinders"
  ).mkString(";")

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages,
    ScoverageKeys.coverageExcludedFiles := excludedFiles,
    ScoverageKeys.coverageMinimumStmtTotal := 90.00,
    ScoverageKeys.coverageMinimumBranchTotal := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )

}
