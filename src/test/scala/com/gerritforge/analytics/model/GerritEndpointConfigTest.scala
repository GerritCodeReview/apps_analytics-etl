// Copyright (C) 2018 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.analytics.model

import java.time.LocalDate

import com.gerritforge.analytics.support.api.{GerritApiAuth, GerritServiceApi}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.io.Source


class GerritEndpointConfigTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

  val wiremockHost: String = "localhost"
  lazy val wiremockPort = wireMockConfig().dynamicPort()
  var randomWireMockPort: Int = _
  lazy val wiremockServer: WireMockServer = new WireMockServer(wiremockPort)

  override def beforeEach(): Unit = {
    wiremockServer.start()

    randomWireMockPort = wiremockServer.port()

    WireMock.configureFor(wiremockHost, randomWireMockPort)
  }

  override def afterEach(): Unit = {
    wiremockServer.stop()
  }

  "gerritProjectsUrl" should "contain prefix when available" in {

    val conf = GerritEndpointConfig(
      since = Some(LocalDate.of(2018, 1, 1)),
      until = Some(LocalDate.of(2018, 4, 30)),
      aggregate = Some("email")
    )
    val projectId = "hello-world"
    conf.contributorsUrl(projectId) shouldBe s"/projects/$projectId/analytics~contributors?since=2018-01-01&until=2018-04-30&aggregate=email"
  }

  "gerrit api client" should "fetch the analytics content" in {

    val path = "/projects/hello-world/analytics~contributors?since=2018-01-01&until=2018-04-30&aggregate=email"

    val responseContent =
      """{"year":2018,"month":4,"day":17,"hour":10,"name":"The User","email":"the.user@company.com","num_commits":3,"num_files":4,"num_distinct_files":2,"added_lines":6,"deleted_lines":1,"commits":[{"sha1":"976821ca2d1324a98a1b30a035456616d5307b05","date":1523960215000,"merge":false,"files":["hello.txt","hello2.txt"]},{"sha1":"02d2787b3862aced9ac13c82b85be98db8fb6900","date":1523959841000,"merge":false,"files":["hello2.txt"]},{"sha1":"f2dd6d322655ce7f28a937f85dcec93d69b98a66","date":1523959613000,"merge":false,"files":["hello.txt"]}],"branches":[],"issues_codes":[],"issues_links":[],"last_commit_date":1523960215000,"is_merge":false}
        |{"year":2018,"month":5,"day":11,"hour":15,"name":"The User","email":"the.user@company.com","num_commits":2,"num_files":2,"num_distinct_files":1,"added_lines":1,"deleted_lines":3,"commits":[{"sha1":"06acddab876fa4f0af5e92ef38ee797ccabc435f","date":1526052064000,"merge":false,"files":["hello2.txt"]},{"sha1":"51f5757d7489b52c2b317aebc9453af2aea7fe78","date":1526052154000,"merge":false,"files":["hello2.txt"]}],"branches":[],"issues_codes":[],"issues_links":[],"last_commit_date":1526052154000,"is_merge":false}""".stripMargin
    stubFor(get(urlEqualTo(path))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(responseContent)
      )
    )

    val request: String = s"http://$wiremockHost:$randomWireMockPort"
    val gerritApiService: GerritServiceApi = new GerritServiceApi(request, None, None)

    Source.fromInputStream(gerritApiService.getContentFrom(path)).mkString should equal(responseContent)
  }

}
