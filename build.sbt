import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "2.24.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.55.0-play-27",
  "uk.gov.hmrc" %% "play-ui" % "8.11.0-play-27",
  "uk.gov.hmrc" %% "auth-client" % "3.0.0-play-27",
  "uk.gov.hmrc" %% "play-partials" % "6.11.0-play-27",
  "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-26",
  "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.19.0-play-27",
  "com.typesafe.play" %% "play-json-joda" % "2.7.4",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "4.4.0",
  "uk.gov.hmrc" %% "play-language" % "4.3.0-play-27"
)

def tmpMacWorkaround(): Seq[ModuleID] =
  if (sys.props.get("os.name").fold(false)(_.toLowerCase.contains("mac")))
    Seq("org.reactivemongo" % "reactivemongo-shaded-native" % "0.18.6-osx-x86-64" % "runtime,test,it")
  else Seq()

def testDeps(scope: String) = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
  "org.scalatest" %% "scalatest" % "3.0.8" % scope,
  "org.mockito" % "mockito-core" % "2.27.0" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.27.1" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "4.21.0-play-27" % scope,
  "org.jsoup" % "jsoup" % "1.12.1" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-mapping-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.10",
    scalacOptions ++= Seq(
      //"-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-value-discard",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions"),
    PlayKeys.playDefaultPort := 9438,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= tmpMacWorkaround ++ compileDeps ++ testDeps("test") ++ testDeps("it"),
    publishingSettings,
    scoverageSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    routesImport ++= Seq("uk.gov.hmrc.agentmappingfrontend.controllers.UrlBinders._"),
    scalafmtOnCompile in Compile := true,
    scalafmtOnCompile in Test := true
  )
  .configs(IntegrationTest)
  .settings(
    majorVersion := 0,
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    scalafmtOnCompile in IntegrationTest := true
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)

inConfig(IntegrationTest)(scalafmtCoreSettings)
