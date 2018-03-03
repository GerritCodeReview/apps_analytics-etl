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

package com.gerritforge.analytics.engine

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}

import com.gerritforge.analytics.model._
import com.gerritforge.analytics.support.api.GerritServiceApi
import org.apache.spark.sql.functions.{udf, _}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

import scala.io.Source

object GerritAnalyticsTransformations {


  def getAliasDF(emailAliases: Option[String])(implicit spark: SparkSession): Option[DataFrame] = {
    emailAliases.map { path =>
      spark.sqlContext.read
        .option("header", "true")
        .option("mode", "DROPMALFORMED")
        .option("inferSchema", "true")
        .csv(path)
        .toDF()
    }
  }

  case class CommitterInfo(author: String, email_alias: String)

  case class CommitInfo(sha1: String, date: Long, merge: Boolean)

  case class UserActivitySummary(year: Integer,
                                 month: Integer,
                                 day: Integer,
                                 hour: Integer,
                                 name: String,
                                 email: String,
                                 num_commits: Integer,
                                 num_files: Integer,
                                 num_distinct_files: Integer,
                                 added_lines: Integer,
                                 deleted_lines: Integer,
                                 commits: Array[CommitInfo],
                                 last_commit_date: Long,
                                 is_merge: Boolean)

  import org.apache.spark.sql.Encoders

  val schema = Encoders.product[UserActivitySummary].schema

  /**
    * Assumes the data frame contains the 'commits' column with an array of CommitInfo in it
    * and returns a DataSet[String] with the commits SHA1
    */
  def extractCommits(df: DataFrame)(implicit spark: SparkSession): Dataset[String] = {
    import spark.implicits._

    df
      .select(explode($"commits.sha1"))
      .as[String]
      .distinct() //might be useless this distinct, just want to be sure I'm respecting the contract
  }

  implicit class PimpedDataFrame(val df: DataFrame) extends AnyVal {
    def transformCommitterInfo()(implicit spark: SparkSession): DataFrame = {
      import org.apache.spark.sql.functions.from_json
      import spark.sqlContext.implicits._
      df.withColumn("json", from_json($"json", schema))
        .selectExpr(
          "project", "json.name as author", "json.email as email",
          "json.year as year", "json.month as month", "json.day as day", "json.hour as hour",
          "json.num_files as num_files", "json.num_distinct_files as num_distinct_files",
          "json.added_lines as added_lines", "json.deleted_lines as deleted_lines",
          "json.num_commits as num_commits", "json.last_commit_date as last_commit_date",
          "json.is_merge as is_merge", "json.commits as commits"
        )
    }

    def handleAliases(aliasesDF: Option[DataFrame])(implicit spark: SparkSession): DataFrame = {
      aliasesDF
        .map { eaDF =>
          val renamedAliasesDF = eaDF
            .withColumnRenamed("email", "email_alias")
            .withColumnRenamed("author", "author_alias")
            .withColumnRenamed("organization", "organization_alias")

          df.join(renamedAliasesDF, df("email") === renamedAliasesDF("email_alias"), "left_outer")
            .withColumn("organization",
              when(renamedAliasesDF("organization_alias").notEqual(""), lower(renamedAliasesDF("organization_alias")))
                .otherwise(df("organization")))
            .withColumn("author", coalesce(renamedAliasesDF("author_alias"), df("author")))
            .drop("email_alias", "author_alias", "organization_alias")
        }
        .getOrElse(df)
    }


    def convertDates(columnName: String)(implicit spark: SparkSession): DataFrame = {
      df.withColumn(columnName, longDateToISOUdf(col(columnName)))
    }

    def dropCommits(implicit spark: SparkSession): DataFrame = {
      df.drop("commits")
    }

    def addOrganization()(implicit spark: SparkSession): DataFrame =
      df.withColumn("organization", emailToDomainUdf(col("email")))

    def commitSet(implicit spark: SparkSession): Dataset[String] = {
      extractCommits(df)
    }

    def dashboardStats(aliasesDFMaybe: Option[DataFrame])(implicit spark: SparkSession): DataFrame = {
      df
        .addOrganization()
        .handleAliases(aliasesDFMaybe)
        .convertDates("last_commit_date")
        .dropCommits
    }
  }

  private def emailToDomain(email: String): String = email match {
    case Email(_, domain) => domain
    case _ => ""
  }

  private def emailToDomainUdf = udf(emailToDomain(_: String))


  import org.apache.spark.sql.functions.udf

  val longDateToISOUdf = udf(longDateToISO(_: Number))

  def longDateToISO(in: Number): String =
    ZonedDateTime.ofInstant(
      LocalDateTime.ofEpochSecond(in.longValue() / 1000L, 0, ZoneOffset.UTC),
      ZoneOffset.UTC, ZoneId.of("Z")
    ) format DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def getContributorStatsFromAnalyticsPlugin(projects: Dataset[GerritProject], lookupFunction: String => String, gerritApiService: GerritServiceApi)(implicit spark: SparkSession): DataFrame = {
    import spark.sqlContext.implicits._

    projects
      .map { project =>
        val builtContributorsURL: String = lookupFunction(project.id)
        val request = gerritApiService.getContentFrom(builtContributorsURL)
        val requestResponse = Source.fromInputStream(request).getLines().mkString
        (project.name, requestResponse)
      }.toDF("project", "json")
      .transformCommitterInfo
  }

}
