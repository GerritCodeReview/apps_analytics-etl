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

import java.io.{File, FileOutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import com.gerritforge.analytics.engine.GerritAnalyticsTransformations._
import com.gerritforge.analytics.model.{GerritProject, GerritProjectsSupport, ProjectContributionSource}
import org.apache.spark.sql.Row
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods.{compact, render}
import org.scalatest.{Inside, Matchers, WordSpec}

import scala.io.Source

case class TransformedColumns
(project: String, author: String, email: String,
 year: Option[Int], month: Option[Int], day: Option[Int], hour: Option[Int],
 num_files: Int, num_distinct_files: Int, added_lines: Int, deleted_lines: Int,
 num_commits: Int, last_commit_date: Long, is_merge: Boolean,
 commits: Array[CommitInfo], branches: Array[String], issues_codes: Array[String],
 issues_links: Array[String])

class GerritAnalyticsTransformationsSpec extends WordSpec with Matchers
  with SparkTestSupport with Inside {

  "GerritProjects" should {
    "parse JSON into a GerritProject objects" in {

      val projects = GerritProjectsSupport.parseJsonProjectListResponse(Source.fromString(
        """)]}'
          |{
          | "All-Projects-name": {
          |   "id": "All-Projects-id",
          | },
          | "Test-name": {
          |   "id": "Test-id",
          | }
          |}
          |""".stripMargin))

      projects should contain only(GerritProject("All-Projects-id", "All-Projects-name"), GerritProject("Test-id", "Test-name"))
    }
  }


  "enrichWithSource" should {
    "enrich project RDD object with its source" in {

      val projectRdd = sc.parallelize(Seq(GerritProject("project-id", "project-name")))

      val projectWithSource = projectRdd
        .enrichWithSource(projectId => s"http://somewhere.com/$projectId")
        .collect

      projectWithSource should have size 1
      inside(projectWithSource.head) {
        case ProjectContributionSource(projectName, url) => {
          projectName should be("project-name")
          url shouldBe "http://somewhere.com/project-id"
        }
      }
    }
  }

  "fetchRawContributors" should {
    "fetch file content from the initial list of project names and file names" in {

      val line1 = "foo" -> "bar"
      val line2 = "foo1" -> "bar1"
      val line3 = "foo2" -> "bar2"
      val line3b = "foo3" -> "bar3"

      val projectSource1 = ProjectContributionSource("p1", newSource(line1, line2, line3))
      val projectSource2 = ProjectContributionSource("p2", newSource(line3b))

      val rawContributors = sc.parallelize(Seq(projectSource1, projectSource2))
        .fetchRawContributors
        .collect

      rawContributors should have size (4)
      rawContributors should contain allOf(
        ("p1","""{"foo":"bar"}"""),
        ("p1","""{"foo1":"bar1"}"""),
        ("p1", """{"foo2":"bar2"}"""),
        ("p2", """{"foo3":"bar3"}""")
      )
    }


    "fetch file content from the initial list of project names and file names with non-latin chars" in {
      val rawContributors = sc.parallelize(Seq(ProjectContributionSource("p1", newSource("foo2" -> "bar2\u0100"))))
        .fetchRawContributors
        .collect

      rawContributors should have size (1)
      rawContributors.head._2 should be("""{"foo2":"bar2\u0100"}""")
    }
  }

  "transformCommitterInfo" should {
    "transform a DataFrame(project,json) to DF with basic columns" in {
      import sql.implicits._

      val rdd = sc.parallelize(Seq(
        ("p1","""{"name":"a","email":"a@mail.com","year":2017,"month":9, "day":11, "hour":23, "num_commits":1, "num_files": 2, "num_distinct_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":0, "is_merge": false, "commits":[{ "sha1": "e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":0,"merge":false, "files": ["file1.txt", "file2.txt"]}] }""")))

      val df = rdd.toDF("project", "json")
        .transformCommitterInfo

      df.count should be(1)


      df.schema.fields.map(_.name) should contain inOrder(
        "project", "author", "email",
        "year", "month", "day", "hour",
        "num_files", "num_distinct_files", "added_lines", "deleted_lines",
        "num_commits", "last_commit_date",
        "is_merge", "commits")

      val collected = df.as[TransformedColumns].collect

      inside(collected.head) {
        case TransformedColumns(project, author, email, year, month, day, hour, num_files, num_distinct_files, added_lines, deleted_lines, num_commits, last_commit_date, is_merge, commits, _, _, _) =>
          project should be("p1")
          email should be("a@mail.com")
          author should be("a")
          year should be(Some(2017))
          month should be(Some(9))
          day should be(Some(11))
          hour should be(Some(23))
          num_commits should be(1)
          added_lines should be(1)
          deleted_lines should be(1)
          last_commit_date should be(0)
          is_merge should be(false)
      }

    }

    "understand optional fields" in {
      import sql.implicits._

      val rdd = sc.parallelize(Seq(
        // last commit is missing hour,day,month,year to check optionality
        ("p3",
          """{"name":"c","email":"c@mail.com","num_commits":12,"num_files": 4, "num_distinct_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":1600000000000,"is_merge": true,"commits":[{"sha1":"e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":0,"merge":true, "files": ["file1.txt", "file2.txt"] },{"sha1":"e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":1600000000000,"merge":true, "files": ["file1.txt", "file2.txt"]}]}"""))
      )

      val df = rdd.toDF("project", "json")
        .transformCommitterInfo

      df.count should be(1)
      val collected = df.as[TransformedColumns].collect

      inside(collected.head) {
        case TransformedColumns(project, author, email, year, month, day, hour, num_files, num_distinct_files, added_lines, deleted_lines, num_commits, last_commit_date, is_merge, commits, _, _, _) =>
          email should be("c@mail.com")
          author should be("c")
          year should be(None)
          month should be(None)
          day should be(None)
          hour should be(None)
          num_commits should be(12)
          added_lines should be(1)
          deleted_lines should be(1)
          last_commit_date should be(1600000000000L)
          is_merge should be(true)
      }
    }

    "recognize branches in input" in {
      import sql.implicits._

      val rdd = sc.parallelize(Seq(
        ("p1",
          """{"name":"a","email":"a@mail.com","year":2017,"month":9, "day":11, "hour":23, "num_commits":1,
            |"num_files": 2, "num_distinct_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":0,
            |"is_merge": false, "commits":[{ "sha1": "e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":0,"merge":false,
            |"files": ["file1.txt", "file2.txt"]}],
            |"branches":["master"] }"""
            .stripMargin)))

      val df = rdd.toDF("project", "json")
        .transformCommitterInfo

      df.count should be(1)
      df.schema.fields.map(_.name) should contain("branches")
      val collected = df.as[TransformedColumns].collect

      inside(collected.head) {
        case TransformedColumns(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, branches, _, _) =>
          branches.size should be(1)
          branches.head should be("master")
      }
    }


    "recognize issues in input" in {
      import sql.implicits._
      val sampleProjectJson = Seq(
        ("p1",
          """{"name":"a","email":"a@mail.com","year":2017,"month":9, "day":11, "hour":23, "num_commits":1,
            |"num_files": 2, "num_distinct_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":0,
            |"is_merge": false, "commits":[{ "sha1": "e063a806c33bd524e89a87732bd3f1ad9a77a41e", "date":0,"merge":false,
            |"files": ["file1.txt", "file2.txt"]}],
            |"issues_codes":["c1"], "issues_links":["http://link/c1"] }"""
            .stripMargin))


      val rdd = sc.parallelize(sampleProjectJson)

      val df = rdd.toDF("project", "json")
        .transformCommitterInfo

      df.count should be(1)
      df.schema.fields.map(_.name) should contain allOf("issues_codes", "issues_links")
      val collected = df.as[TransformedColumns].collect

      inside(collected.head) {
        case TransformedColumns(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, issues_codes, issues_links) =>
          issues_codes.size should be(1)
          issues_codes.head should be("c1")
          issues_links.size should be(1)
          issues_links.head should be("http://link/c1")
      }
    }
  }

  "handleAliases" should {
    "enrich the data with author from the alias DF" in {
      import spark.implicits._

      val aliasDF = sc.parallelize(Seq(
        ("aliased_author", "aliased_email@aliased_author.com", "")
      )).toDF("author", "email", "organization")

      val inputSampleDF = sc.parallelize(Seq(
        ("author_from_name_a", "non_aliased_email@a_mail.com", "a_mail.com"),
        ("author_from_name_b", "aliased_email@aliased_author.com", "aliased_author.com")
      )).toDF("author", "email", "organization")

      val expectedDF = sc.parallelize(Seq(
        ("author_from_name_a", "non_aliased_email@a_mail.com", "a_mail.com"),
        ("aliased_author", "aliased_email@aliased_author.com", "aliased_author.com")
      )).toDF("author", "email", "organization")

      val df = inputSampleDF.handleAliases(Some(aliasDF))

      df.schema.fields.map(_.name) should contain allOf(
        "author", "email", "organization")

      df.collect should contain theSameElementsAs expectedDF.collect
    }
    "enrich the data with organization from the alias DF when available" in {
      import spark.implicits._

      val aliasDF = sc.parallelize(Seq(
        ("aliased_author_with_organization", "aliased_email@aliased_organization.com", "aliased_organization"),
        ("aliased_author_empty_organization", "aliased_email@emtpy_organization.com", ""),
        ("aliased_author_null_organization", "aliased_email@null_organization.com", null)

      )).toDF("author", "email", "organization")

      val inputSampleDF = sc.parallelize(Seq(
        ("author_from_name_a", "aliased_email@aliased_organization.com", "aliased_organization.com"),
        ("author_from_name_b", "aliased_email@emtpy_organization.com", "emtpy_organization.com"),
        ("author_from_name_c", "aliased_email@null_organization.com", "null_organization.com")
      )).toDF("author", "email", "organization")

      val expectedDF = sc.parallelize(Seq(
        ("aliased_author_with_organization", "aliased_email@aliased_organization.com", "aliased_organization"),
        ("aliased_author_empty_organization", "aliased_email@emtpy_organization.com", "emtpy_organization.com"),
        ("aliased_author_null_organization", "aliased_email@null_organization.com", "null_organization.com")
      )).toDF("author", "email", "organization")

      val df = inputSampleDF.handleAliases(Some(aliasDF))

      df.schema.fields.map(_.name) should contain allOf(
        "author", "email", "organization")

      df.collect should contain theSameElementsAs expectedDF.collect
    }
    "return correct columns when alias DF is defined" in {
      import spark.implicits._
      val inputSampleDF = sc.parallelize(Seq(
        ("author_name", "email@mail.com", "an_organization")
      )).toDF("author", "email", "organization")

      val aliasDF = sc.parallelize(Seq(
        ("a_random_author", "a_random_email@mail.com", "a_random_organization")
      )).toDF("author", "email", "organization")

      val df = inputSampleDF.handleAliases(Some(aliasDF))

      df.schema.fields.map(_.name) should contain allOf("author", "email", "organization")
    }

    "return correct columns when alias DF is not defined" in {
      import spark.implicits._
      val inputSampleDF = sc.parallelize(Seq(
        ("author_name", "email@mail.com", "an_organization")
      )).toDF("author", "email", "organization")

      val df = inputSampleDF.handleAliases(None)

      df.schema.fields.map(_.name) should contain allOf("author", "email", "organization")
    }

    "lowercase aliased organizations" in {
      import spark.implicits._
      val inputSampleDF = sc.parallelize(Seq(
        ("author_name", "email@mail.com", "an_organization")
      )).toDF("author", "email", "organization")

      val aliasDF = sc.parallelize(Seq(
        ("author_name", "email@mail.com", "OrGaNiZaTiOnToBeLoWeRcAsEd")
      )).toDF("author", "email", "organization")

      val df = inputSampleDF.handleAliases(Some(aliasDF))

      val expectedDF = sc.parallelize(Seq(
        ("author_name", "email@mail.com", "organizationtobelowercased")
      )).toDF("author", "email", "organization")

      df.collect should contain theSameElementsAs expectedDF.collect
    }
  }
  "addOrganization" should {
    "compute organization column from the email" in {
      import sql.implicits._

      val df = sc.parallelize(Seq(
        "",
        "@", // corner case
        "not an email",
        "email@domain-simple",
        "email@domain-com.com",
        "email@domain-couk.co.uk",
        "email@domain-info.info",
        "email@mail.companyname-couk.co.uk",
        "email@mail.companyname-com.com",
        "email@mail.companyname-info.info"

      )).toDF("email")

      val transformed = df.addOrganization()

      transformed.schema.fields.map(_.name) should contain allOf("email", "organization")

      transformed.collect should contain allOf(
        Row("", ""),
        Row("@", ""),
        Row("not an email", ""),
        Row("email@domain-simple", "domain-simple"),
        Row("email@domain-com.com", "domain-com"),
        Row("email@domain-couk.co.uk", "domain-couk"),
        Row("email@domain-info.info", "domain-info"),
        Row("email@mail.companyname-couk.co.uk", "mail.companyname-couk"),
        Row("email@mail.companyname-com.com", "mail.companyname-com"),
        Row("email@mail.companyname-info.info", "mail.companyname-info")
      )
    }
  }

  "convertDates" should {
    "process specific column from Long to ISO date" in {
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
  }

  "extractCommitsPerProject" should {
    "generate a Dataset with the all the SHA of commits with associated project" in {
      import sql.implicits._

      val committerInfo = sc.parallelize(Seq(
        ("p1","""{"name":"a","email":"a@mail.com","year":2017,"month":9, "day":11, "hour":23, "num_commits":1, "num_files": 2, "num_distinct_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":0, "is_merge": false, "commits":[{ "sha1": "sha_1", "date":0,"merge":false, "files": ["file1.txt", "file2.txt"]}] }"""),
        ("p2","""{"name":"b","email":"b@mail.com","year":2017,"month":9, "day":11, "hour":23, "num_commits":428, "num_files": 2, "num_distinct_files": 3, "added_lines":1, "deleted_lines":1, "last_commit_date":1500000000000, "is_merge": true, "commits":[{"sha1":"sha_2", "date":0,"merge":true, "files": ["file3.txt", "file4.txt"] },{"sha1":"sha_3", "date":1500000000000,"merge":true, "files": ["file1.txt", "file4.txt"]}]}"""),
        // last commit is missing hour,day,month,year to check optionality
        ("p3","""{"name":"c","email":"c@mail.com","num_commits":12,"num_files": 4, "num_distinct_files": 2, "added_lines":1, "deleted_lines":1, "last_commit_date":1600000000000,"is_merge": true,"commits":[{"sha1":"sha_4", "date":0,"merge":true, "files": ["file1.txt", "file2.txt"] },{"sha1":"sha_5", "date":1600000000000,"merge":true, "files": ["file1.txt", "file2.txt"]}]}""")
      )).toDF("project", "json")
        .transformCommitterInfo

      committerInfo.commitSet.collect() should contain only(
        "sha_1",
        "sha_2",
        "sha_3",
        "sha_4",
        "sha_5"
      )

    }
  }

  private def newSource(contributorsJson: JObject*): String = {
    val tmpFile = File.createTempFile(System.getProperty("java.io.tmpdir"),
      s"${getClass.getName}-${System.nanoTime()}")

    val out = new OutputStreamWriter(new FileOutputStream(tmpFile), StandardCharsets.UTF_8)
    contributorsJson.foreach(json => out.write(compact(render(json)) + '\n'))
    out.close
    tmpFile.toURI.toString
  }
}
