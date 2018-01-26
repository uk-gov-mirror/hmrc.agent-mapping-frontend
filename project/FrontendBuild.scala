import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "agent-mapping-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test() ++ test("it")

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" % "8.17.0",
    "uk.gov.hmrc" %% "play-partials" % "6.1.0",
    "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.10.0",
    "uk.gov.hmrc" %% "passcode-verification" % "5.1.0",
    "uk.gov.hmrc" %% "auth-client" % "2.5.0",
    "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "2.4.0"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.4.0" % scope,
    "com.github.tomakehurst" % "wiremock" % "2.14.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.11.2" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
    "org.mockito" % "mockito-core" % "2.13.0" % scope
  )

}
