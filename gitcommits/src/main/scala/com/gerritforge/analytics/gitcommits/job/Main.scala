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

package com.gerritforge.analytics.gitcommits.job


import java.time.LocalDate

import com.gerritforge.analytics.gitcommits.model.{GerritEndpointConfig, GerritProject, GerritProjectsSupport}
import com.gerritforge.analytics.spark.SparkApp
import com.gerritforge.analytics.support.ops.ReadsOps._
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import scopt.OptionParser

import scala.io.Codec

object Main extends App with SparkApp with Job with LazyLogging with FetchRemoteProjects {
  override val appName = "Gerrit GitCommits Analytics ETL"

  private val fileExists: String => Either[String, Unit] = { path =>
    if (!new java.io.File(path).exists) {
      Left(s"ERROR: Path '$path' doesn't exists!")
    } else {
      Right()
    }
  }

  private val cliOptionParser: OptionParser[GerritEndpointConfig] =
    new scopt.OptionParser[GerritEndpointConfig]("scopt") {
      head("scopt", "3.x")
      opt[String]('u', "url") optional () action { (x, c) =>
        c.copy(baseUrl = Some(x))
      } text "gerrit url"
      opt[String]('p', "prefix") optional () action { (p, c) =>
        c.copy(prefix = Some(p))
      } text "projects prefix"
      opt[String]('o', "out") optional () action { (x, c) =>
        c.copy(outputDir = x)
      } text "output directory"
      opt[String]('e', "elasticIndex") optional () action { (x, c) =>
        c.copy(elasticIndex = Some(x))
      } text "index name"
      opt[LocalDate]('s', "since") optional () action { (x, c) =>
        c.copy(since = Some(x))
      } text "begin date "
      opt[LocalDate]('u', "until") optional () action { (x, c) =>
        c.copy(until = Some(x))
      } text "since date"
      opt[String]('g', "aggregate") optional () action { (x, c) =>
        c.copy(aggregate = Some(x))
      } text "aggregate email/email_hour/email_day/email_month/email_year"

      opt[String]('a', "email-aliases") optional () validate fileExists action { (path, c) =>
        c.copy(emailAlias = Some(path))
      } text "\"emails to author alias\" input data path"

      opt[String]("username") optional () action { (input, c) =>
        c.copy(username = Some(input))
      } text "Gerrit API Username"

      opt[String]("password") optional () action { (input, c) =>
        c.copy(password = Some(input))
      } text "Gerrit API Password"

      opt[Boolean]('k', "ignore-ssl-cert") optional () action { (input, c) =>
        c.copy(ignoreSSLCert = Some(input))
      } text "Ignore SSL certificate validation"

      opt[Boolean]('r', "extract-branches") optional () action { (input, c) =>
        c.copy(extractBranches = Some(input))
      } text "enables branches extraction for each commit"

      opt[Boolean]('t', "extract-hashtags") optional () action { (input, c) =>
        c.copy(extractHashTags = Some(input))
      } text "enables hashtags extraction for each change"

    }

  cliOptionParser.parse(args, GerritEndpointConfig()) match {
    case Some(config) =>

      implicit val _: GerritEndpointConfig = config

      logger.info(s"Starting analytics app with config $config")

      val dataFrame = buildProjectStats().cache() //This dataframe is written twice

      logger.info(s"ES content created, saving it to '${config.outputDir}'")
      dataFrame.write.json(config.outputDir)

      saveES(dataFrame)

    case None => // invalid configuration usage has been displayed
  }
}

trait Job {
  self: LazyLogging with FetchProjects =>
  implicit val codec = Codec.ISO8859

  val indexType = "gitCommits"

  def buildProjectStats()(implicit config: GerritEndpointConfig, spark: SparkSession): DataFrame = {
    import com.gerritforge.analytics.gitcommits.engine.GerritAnalyticsTransformations._

    implicit val sc: SparkContext = spark.sparkContext

    val projects = fetchProjects(config)

    logger.info(
      s"Loaded a list of ${projects.size} projects ${if (projects.size > 20) projects.take(20).mkString("[", ",", ", ...]")
      else projects.mkString("[", ",", "]")}")

    val aliasesDF = getAliasDF(config.emailAlias)

    val contributorsStats = getContributorStats(spark.sparkContext.parallelize(projects),
                                             config.contributorsUrl,
                                             config.gerritApiConnection)
    contributorsStats.dashboardStats(aliasesDF)
  }

  def saveES(df: DataFrame)(implicit config: GerritEndpointConfig) {
    import org.elasticsearch.spark.sql._
    config.elasticIndex.foreach { esIndex =>
      logger.info(
        s"ES content created, saving it to elastic search instance at '${config.elasticIndex}/$indexType'")

      df.saveToEs(s"$esIndex/$indexType")
    }

  }
}

trait FetchProjects {
  def fetchProjects(config: GerritEndpointConfig): Seq[GerritProject]
}

trait FetchRemoteProjects extends FetchProjects {
  def fetchProjects(config: GerritEndpointConfig): Seq[GerritProject] = {
    config.gerritProjectsUrl.toSeq.flatMap { url =>
      GerritProjectsSupport.parseJsonProjectListResponse(
        config.gerritApiConnection.getContentFromApi(url))
    }
  }
}
