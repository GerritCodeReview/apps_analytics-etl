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

import java.io.{File, FileWriter}

import com.gerritforge.analytics.model.{GerritEndpointConfig, ProjectContribution, ProjectContributionSource}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{FlatSpec, Inside, Matchers, PartialFunctionValues}

class GerritAnalyticsTransformationsSpec extends FlatSpec with Matchers with SparkTestSupport with Inside with PartialFunctionValues {

  import com.gerritforge.analytics.engine.GerritAnalyticsTrasformations._

  "A project" should "be enriched with its contributors URL endpoint" in {
    withSparkContext { sc =>
      val projectRdd = sc.parallelize(Seq("project"))
      val config = GerritEndpointConfig("http://somewhere.com")

      val projectWithSource = projectRdd.enrichWithSource(config).collect

      projectWithSource should have size 1

      inside(projectWithSource.head) {
        case ProjectContributionSource(project, url) => {
          project should be("project")
          url should startWith("http://somewhere.com")
        }
      }
    }
  }

  it should "return two lines enriched with its two contributors" in {
    val contributor1: JObject = ("foo" -> "bar")
    val contributor2: JObject = ("first" -> "John")
    val projectSource = ProjectContributionSource("project", newSource(contributor1, contributor2))

    withSparkContext { sc =>
      val projectContributions = sc.parallelize(Seq(projectSource))
        .fetchContributors
        .collect()

      projectContributions should contain allOf(
        ProjectContribution("project", contributor1),
        ProjectContribution("project", contributor2)
      )
    }
  }

  "A ProjectContribution RDD" should "serialize to Json" in {
    val pc1 = ProjectContribution("project", ("foo" -> "bar"))
    val pc2 = ProjectContribution("project", ("foo2" -> "bar2"))
    withSparkContext { sc =>
      sc.parallelize(Seq(pc1, pc2)).toJson.collect should contain allOf(
        """{"project":"project","foo":"bar"}""",
        """{"project":"project","foo2":"bar2"}""")
    }
  }

  "getFileContentAsProjectContributions" should "collect contributors and handle utf-8" in {
    val contributor1: JObject = ("foo" -> "bar")
    val contributor2: JObject = ("first" -> "(A with macron) in unicode is: \u0100")
    val url = newSource(contributor1, contributor2)
    val projectContributions = getFileContentAsProjectContributions(url, "project").toArray

    projectContributions should contain allOf(
      ProjectContribution("project", contributor1),
      ProjectContribution("project", contributor2)
    )
  }

  "emailToDomain" should "parse domain" in {
    emailToDomain.valueAt("a@x.y") should be ("x.y")
  }

  "transformAddOrganization" should "add organization" in {
    val contributor: JObject = ("name" -> "contributor1") ~ ("email" -> "name1@domain1")
    val transformed = transformAddOrganization(contributor)
    transformed \ "organization" should be(JString("domain1"))
  }

  private def newSource(contributorsJson: JObject*): String = {
    val tmpFile = File.createTempFile(System.getProperty("java.io.tmpdir"),
      s"${getClass.getName}-${System.nanoTime()}")

    val out = new FileWriter(tmpFile)
    contributorsJson.foreach(json => out.write(compact(render(json)) + '\n'))
    out.close
    tmpFile.toURI.toString
  }
}
