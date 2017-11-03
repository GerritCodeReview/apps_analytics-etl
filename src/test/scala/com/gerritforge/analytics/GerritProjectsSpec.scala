// Copyright (C) 2017 GerritForge Ltd
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

package com.gerritforge.analytics

import com.gerritforge.analytics.model.GerritProjects
import org.scalatest.{FlatSpec, Matchers}

import scala.io.{Codec, Source}

class GerritProjectsSpec extends FlatSpec with Matchers {

  "GerritProjects" should "parseProjects" in {
    val projectNames = GerritProjects(Source.fromString(
      """)]}'
        |{
        | "All-Projects": {
        |   "id": "All-Projects",
        |   "state": "ACTIVE"
        | },
        | "Test": {
        |   "id": "Test",
        |   "state": "ACTIVE"
        | }
        |}
        |""".stripMargin))

    projectNames should have size 2
    projectNames should contain allOf("All-Projects", "Test")
  }

}
