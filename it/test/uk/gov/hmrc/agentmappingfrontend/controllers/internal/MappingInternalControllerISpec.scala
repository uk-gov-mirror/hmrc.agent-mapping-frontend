/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.controllers.internal

import com.google.inject.AbstractModule
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.model.identifiers.Arn
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport

class MappingInternalControllerISpec
extends BaseControllerISpec
with AuthStubs
with MongoSupport {

  override def additionalConfig: Map[String, String] = Map("mongodb.uri" -> mongoUri)

  override def moduleWithOverrides: AbstractModule =
    new AbstractModule {
      override def configure(): Unit = bind(classOf[MongoComponent]).toInstance(mongoComponent)
    }

  val arn: Arn = Arn("TARN0000001")

  def callEndpointWith[A: Writeable](request: Request[A]): Result = await(play.api.test.Helpers.route(app, request).get)

  "GET /start-auth-mapping-journey" should {
    "redirect to agent code page and set up journey if user is ASA agent" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = FakeRequest(POST, "/start-auth-mapping-journey")
        .withSession(SessionKeys.authToken -> "Bearer XYZ")
        .withBody(Json.obj(
          "clientName" -> "Client Name",
          "clientsLegacyRelationships" -> Json.arr(saAgentCode),
          "backUrl" -> "/back-url",
          "cancelUrl" -> "/cancel-url"
        ))
      val result = callEndpointWith(request)
      status(result) shouldBe 201
      val record = repo.collection.find().toFuture().futureValue
      bodyOf(result) shouldBe Json.obj(
        "redirectUrl" -> s"http://localhost:9438${routes.MappingController.showAgentCode(record.head.id).url}"
      ).toString()
    }

    "return forbidden if user is not ASA agent" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = FakeRequest(POST, "/start-auth-mapping-journey")
        .withSession(SessionKeys.authToken -> "Bearer XYZ")
        .withBody(Json.obj(
          "clientName" -> "Client Name",
          "clientsLegacyRelationships" -> Json.arr(saAgentCode),
          "backUrl" -> "/back-url",
          "cancelUrl" -> "/cancel-url"
        ))
      val result = callEndpointWith(request)
      status(result) shouldBe 403
    }
  }

}
