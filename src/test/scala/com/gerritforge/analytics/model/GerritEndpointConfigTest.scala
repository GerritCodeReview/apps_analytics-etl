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

import org.scalatest.{FlatSpec, Matchers}

//class GerritEndpointConfigTest extends FlatSpec with Matchers {
//
//  "gerritProjectsUrl" should "contain prefix when available" in {
//    val prefix = "prefixMustBeThere"
//    val conf = GerritEndpointConfig(baseUrl = "testBaseUrl", prefix = Some(prefix))
//    conf.gerritProjectsUrl shouldBe s"testBaseUrl/projects/?p=$prefix"
//  }
//
//  it should "not contain prefix when not available" in {
//    val conf = GerritEndpointConfig(baseUrl = "testBaseUrl", prefix = None)
//    conf.gerritProjectsUrl shouldBe s"testBaseUrl/projects/"
//  }
//
//  it should "contain authorisation prefix when credentials are available" in {
//    val conf = GerritEndpointConfig(baseUrl = "testBaseUrl", maybeUsername = Some("user"), maybePassword = Some("pwd"))
//    conf.gerritProjectsUrl shouldBe s"testBaseUrl/a/projects/"
//  }
//
//  it should "not contain authorisation prefix when credentials are not available" in {
//    val conf = GerritEndpointConfig(baseUrl = "testBaseUrl", maybeUsername = None, maybePassword = None)
//    conf.gerritProjectsUrl shouldBe s"testBaseUrl/projects/"
//  }
//
//}
