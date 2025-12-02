import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapVer: String = "10.4.0"
  private val mongoVer: String = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-30" % bootstrapVer,
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-30" % "12.21.0",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-30"         % mongoVer,
    "uk.gov.hmrc"        %% "domain-play-30"             % "11.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVer % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVer     % Test
  )
}
