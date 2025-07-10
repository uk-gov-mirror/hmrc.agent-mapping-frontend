import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapVer: String = "9.13.0"
  private val mongoVer: String = "2.6.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-30" % bootstrapVer,
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-30" % "12.7.0",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-30"         % mongoVer,
    "uk.gov.hmrc"        %% "agent-mtd-identifiers"      % "2.2.0",
    "com.github.blemale" %% "scaffeine"                  % "5.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVer % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVer     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.2"      % Test,
    "org.mockito"            %% "mockito-scala-scalatest" % "2.0.0"    % Test
  )
}
