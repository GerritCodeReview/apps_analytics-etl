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

import com.gerritforge.analytics.engine.GerritAnalyticsTransformations._
import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProjects, ProjectContributionSource}
import org.apache.spark.sql.Row
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods.{compact, render}
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.io.Source

class GerritAnalyticsTransformationsSpec extends FlatSpec with Matchers
  with SparkTestSupport with Inside {

  "GerritProjects" should "parse project names" in {

    val projectNames = GerritProjects(Source.fromString(
      """)]}'
        |{
        | "All-Projects": {
        |   "id": "All-Projects",
        | },
        | "Test": {
        |   "id": "Test",
        | }
        |}
        |""".stripMargin))

    projectNames should have size 2
    projectNames should contain allOf("All-Projects", "Test")
  }


  "enrichWithSource" should "enrich project RDD object with its source" in {

    val projectRdd = sc.parallelize(Seq("project"))
    implicit val config = GerritEndpointConfig("http://somewhere.com")

    val projectWithSource = projectRdd
      .enrichWithSource
      .collect

    projectWithSource should have size 1
    inside(projectWithSource.head) {
      case ProjectContributionSource(project, url) => {
        project should be("project")
        url should startWith("http://somewhere.com")
      }
    }
  }

  "fetchRawContributors" should "fetch file content from the initial list of project names and file names" in {

    val line1 = "foo" -> "bar"
    val line2 = "foo1" -> "bar1"
    val line3 = "foo2" -> "bar2\u0100" // checks UTF-8 as well
    val line3b = "foo3" -> "bar3\u0101"

    val projectSource1 = ProjectContributionSource("p1", newSource(line1, line2, line3))
    val projectSource2 = ProjectContributionSource("p2", newSource(line3b))

    val rawContributors = sc.parallelize(Seq(projectSource1, projectSource2))
      .fetchRawContributors
      .collect

    rawContributors should have size (4)
    rawContributors should contain allOf(
      ("p1","""{"foo":"bar"}"""),
      ("p1","""{"foo1":"bar1"}"""),
      ("p1", "{\"foo2\":\"bar2\u0100\"}"),
      ("p2", "{\"foo3\":\"bar3\u0101\"}")
    )
  }

  "transformCommitterInfo" should "transform a DataFrame with project and json to a workable DF with separated columns" in {
    import sql.implicits._

    val rdd = sc.parallelize(Seq(
      ("p1","""{"name":"a","email":"a@mail.com","year":2017,"month":9, "day":11, "hour":23, "num_commits":1, "num_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":0, "commits":[{ "sha1": "e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":0,"merge":false}]}"""),
      ("p2","""{"name":"b","email":"b@mail.com","year":2017,"month":9, "day":11, "hour":23, "num_commits":428, "num_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":1500000000000,"commits":[{"sha1":"e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":0,"merge":true },{"sha1":"e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":1500000000000,"merge":true}]}"""),
      // last commit is missing hour,day,month,year to check optionality
      ("p3","""{"name":"c","email":"c@mail.com","num_commits":12,"num_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":1600000000000,"commits":[{"sha1":"e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":0,"merge":true },{"sha1":"e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":1600000000000,"merge":true}]}""")
    ))

    val df = rdd.toDF("project", "json")
      .transformCommitterInfo

    df.count should be(3)
    val collected = df.collect

    df.schema.fields.map(_.name) should contain allOf(
      "project", "author", "email",
      "year", "month", "day", "hour",
      "num_files", "added_lines", "deleted_lines",
      "num_commits", "last_commit_date")

    collected should contain allOf(
      Row("p1", "a", "a@mail.com", 2017, 9, 11, 23, 2, 1, 1, 1, 0),
      Row("p2", "b", "b@mail.com", 2017, 9, 11, 23, 2, 1, 1, 428, 1500000000000L),
      Row("p3", "c", "c@mail.com", null, null, null, null, 2, 1, 1, 12, 1600000000000L)
    )
  }

  "handleAuthorEMailAliases" should "enrich the data with author from the alias DF" in {
    import spark.implicits._
    val goodAliasDF = sc.parallelize(Seq(
      ("aliased_author", "aliased_email@mail.com")
    )).toDF("author", "email")

    val inputSampleDF = sc.parallelize(Seq(
      ("author_from_name_a", "non_aliased_email@mail.com"),
      ("author_from_name_b", "aliased_email@mail.com")
    )).toDF("author", "email")

    val expectedDF = sc.parallelize(Seq(
      ("author_from_name_a", "non_aliased_email@mail.com"),
      ("aliased_author", "aliased_email@mail.com")
    )).toDF("author", "email")

    val df = inputSampleDF.handleAuthorEMailAliases(Some(goodAliasDF))

    df.schema.fields.map(_.name) should contain allOf(
      "author", "email")

    df.collect should contain theSameElementsAs expectedDF.collect
  }

  it should "return correct columns when alias DF is defined" in {
    import spark.implicits._
    val inputSampleDF = sc.parallelize(Seq(
      ("author_name", "email@mail.com")
    )).toDF("author", "email")

    val aliasDF = sc.parallelize(Seq(
      ("a_random_author", "a_random_email@mail.com")
    )).toDF("author", "email")

    val df = inputSampleDF.handleAuthorEMailAliases(Some(aliasDF))

    df.schema.fields.map(_.name) should contain allOf("author", "email")
  }

  it should "return correct columns when alias DF is not defined" in {
    import spark.implicits._
    val inputSampleDF = sc.parallelize(Seq(
      ("author_name", "email@mail.com")
    )).toDF("author", "email")

    val df = inputSampleDF.handleAuthorEMailAliases(None)

    df.schema.fields.map(_.name) should contain allOf("author", "email")
  }

  "addOrganization" should "compute organization column from the email" in {
    import sql.implicits._

    val df = sc.parallelize(Seq(
      "",
      "@", // corner case
      "not an email",
      "email@domain"
    )).toDF("email")

    val transformed = df.addOrganization()

    transformed.schema.fields.map(_.name) should contain allOf("email", "organization")

    transformed.collect should contain allOf(
      Row("", ""),
      Row("@", ""),
      Row("not an email", ""),
      Row("email@domain", "domain")
    )

  }

  "convertDates" should "process specific column from Long to ISO date" in {
    // some notable dates converted in UnixMillisecs and ISO format
    val DATES = Map(
      0L -> "1970-01-01T00:00:00Z",
      1500000000000L -> "2017-07-14T02:40:00Z",
      1600000000000L -> "2020-09-13T12:26:40Z")
    import sql.implicits._
    val df = sc.parallelize(Seq(
      ("a", 0L, 1),
      ("b", 1500000000000L, 2),
      ("c", 1600000000000L, 3))).toDF("name", "date", "num")

    val converted = df
      .convertDates("date")

    converted.collect should contain allOf(
      Row("a", DATES(0), 1),
      Row("b", DATES(1500000000000L), 2),
      Row("c", DATES(1600000000000L), 3)
    )
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
