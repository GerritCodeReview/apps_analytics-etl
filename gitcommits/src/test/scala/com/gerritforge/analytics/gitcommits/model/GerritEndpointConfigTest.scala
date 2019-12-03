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

package com.gerritforge.analytics.gitcommits.model

import org.scalatest.{FlatSpec, Matchers}
import com.gerritforge.analytics.gitcommits.model.GerritEndpointConfig._
import com.typesafe.config.ConfigFactory

class GerritEndpointConfigTest extends FlatSpec with Matchers {

  import collection.JavaConverters._

  "gerritProjectsUrl" should "contain prefix when available" in {
    val prefix = "prefixMustBeThere"
    val conf = ConfigFactory.parseMap(Map[String, Any](BASE_URL -> "testBaseUrl", PREFIX -> prefix).asJava)
    conf.gerritProjectsUrl should contain(s"testBaseUrl/projects/?p=$prefix")
  }

  it should "not contain prefix when not available" in {
    val conf = ConfigFactory.parseMap(Map[String, Any](BASE_URL -> "testBaseUrl").asJava)
    conf.gerritProjectsUrl should contain(s"testBaseUrl/projects/")
  }

}
