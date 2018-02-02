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

package com.gerritforge.analytics.job

import com.gerritforge.analytics.engine.GerritAnalyticsTransformations._
import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProjectsRDD}
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.io.{Codec, Source}

object Main extends App with Job with LazyLogging {

  new scopt.OptionParser[GerritEndpointConfig]("scopt") {
    head("scopt", "3.x")
    opt[String]('u', "url") optional() action { (x, c) =>
      c.copy(baseUrl = x)
    } text "gerrit url"
    opt[String]('p', "prefix") optional() action { (p, c) =>
      c.copy(prefix = Some(p))
    } text "projects prefix"
    opt[String]('o', "out") optional() action { (x, c) =>
      c.copy(outputDir = x)
    } text "output directory"
    opt[String]('e', "elasticIndex") optional() action { (x, c) =>
      c.copy(elasticIndex = Some(x))
    } text "index name"
    opt[String]('s', "since") optional() action { (x, c) =>
      c.copy(since = Some(x))
    } text "begin date "
    opt[String]('u', "until") optional() action { (x, c) =>
      c.copy(until = Some(x))
    } text "since date"
    opt[String]('g', "aggregate") optional() action { (x, c) =>
      c.copy(aggregate = Some(x))
    } text "aggregate email/email_hour/email_day/email_month/email_year"
    opt[String]('a', "email-aliases") optional() action { (path, c) =>
      if (!new java.io.File(path).exists) {
        println(s"ERROR: Path '${path}' doesn't exists!")
        System.exit(1)
      }
      c.copy(emailAlias = Some(path))
    } text "\"emails to author alias\" input data path"
    opt[Unit]('r',"extract-branches") optional() action { (_, c) =>
      c.copy(extractBranches = true)
    } text "extract branches"
    opt[Unit]('i',"extract-issues") optional() action { (_, c) =>
      c.copy(extractIssues = true)
    } text "extract issues"

  }.parse(args, GerritEndpointConfig()) match {
    case Some(config) =>
      implicit val spark: SparkSession = SparkSession.builder()
        .appName("Gerrit Analytics ETL")
        .getOrCreate()

      implicit val _: GerritEndpointConfig = config

      logger.info(s"Starting analytics app with config $config")

      val dataFrame = run()

      logger.info(s"ES content created, saving it to '${config.outputDir}'")
      dataFrame.write.json(config.outputDir)

      saveES(dataFrame)

    case None => // invalid configuration usage has been displayed
  }
}

trait Job { self: LazyLogging =>
  implicit val codec = Codec.ISO8859

  def run()(implicit config: GerritEndpointConfig, spark: SparkSession): DataFrame = {
    import spark.sqlContext.implicits._ // toDF
    implicit val sc: SparkContext = spark.sparkContext

    val projects = GerritProjectsRDD(Source.fromURL(config.gerritProjectsUrl))

    val aliasesDF = getAliasDF(config.emailAlias)

    projects
      .enrichWithSource
      .fetchRawContributors
      .toDF("project", "json")
      .transformCommitterInfo
      .addOrganization()
      .handleAliases(aliasesDF)
      .convertDates("last_commit_date")

  }

  def saveES(df: DataFrame)(implicit config: GerritEndpointConfig) {
    import org.elasticsearch.spark.sql._
    config.elasticIndex.foreach { esIndex =>
      logger.info(s"ES content created, saving it to elastic search instance at '${config.elasticIndex}'")

      df.saveToEs(esIndex)
    }

  }
}

