import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}


val appName = "agent-mapping-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.16"

val scalaCOptions = Seq(
  "-Werror",
  "-Wdead-code",
  "-Wunused",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:msg=match may not be exhaustive:s", // silence warnings about non-exhaustive pattern matching

)

lazy val root = (project in file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9438,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    TwirlKeys.templateImports ++= twirlImports,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    routesImport ++= Seq("uk.gov.hmrc.agentmappingfrontend.controllers.UrlBinders._"),
    scalacOptions ++= scalaCOptions,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    Test / logBuffered := false,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(
    Test / parallelExecution := false,
    CoverageSettings.settings
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

lazy val twirlImports: Seq[String] = Seq(
  "uk.gov.hmrc.agentmappingfrontend.config.AppConfig",
  "play.api.mvc.RequestHeader",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    Test / logBuffered := false
  )

