package uk.gov.hmrc.agentmappingfrontend.controllers

class CheckMappingControllerWhitelistingISpec extends BaseControllerISpec {
  override protected def passcodeAuthenticationEnabled = true

  private lazy val controller = app.injector.instanceOf[MappingController]

  "root" should {
    behave like aWhitelistedEndpoint(request => controller.root(request))
  }

  "start" should {
    behave like aWhitelistedEndpoint(request => controller.start(request))
  }

  "showCheckAgencyStatus" should {
    behave like aWhitelistedEndpoint(request => controller.submitAddCode(request))
  }
}
