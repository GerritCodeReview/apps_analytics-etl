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
import java.util.Properties

import com.gerritforge.analytics.common.api.SaveMode
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
      cmd("saveToEs").optional()
        .action((_, c) => c.copy(saveMode = SaveMode.SAVE_TO_ES))
        .children(
          opt[String]('e', "elasticSearchIndex") required() action { (input, c) =>
            c.copy(elasticIndex = Some(input))
          } text "elasticSearch index to persist data into (Required)"

        )
      cmd("saveToDb").optional()
        .action((_, c) => c.copy(saveMode = SaveMode.SAVE_TO_DB))
        .children(
          opt[String]('j', "jdbcConnection") required() action { (input, c) =>
            c.copy(jdbcConnection = Some(input))
          } text "Jdbc connection string",
          opt[String]('t', "tableName") required() action { (input, c) =>
            c.copy(tableName = Some(input))
          } text "Database table name",
          opt[String]('d', "driverClass") optional() action { (input, c) =>
            c.copy(driverClassName = Some(input))
          } text "Database jdbc driver class name"
        )
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
      checkConfig(config => if(config.elasticIndex.isEmpty && config.jdbcConnection.isEmpty) Left("saveToEs or saveToDb command must be specified") else Right())
    }

  cliOptionParser.parse(args, GerritEndpointConfig()) match {
    case Some(config) =>
      implicit val _: GerritEndpointConfig = config

      logger.info(s"Starting analytics app with config $config")

      val dataFrame = buildProjectStats().cache() //This dataframe is written twice

      logger.info(s"ES content created, saving it to '${config.outputDir}'")
      dataFrame.write.json(config.outputDir)

      save(dataFrame)

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

  def save(df: DataFrame)(implicit config: GerritEndpointConfig) {
    import scala.concurrent.ExecutionContext.Implicits.global
    config.saveMode match {
      case SaveMode.SAVE_TO_ES =>
        config.elasticIndex.foreach { esIndex =>
          import com.gerritforge.analytics.infrastructure.ESSparkWriterImplicits.withAliasSwap
          df.saveToEsWithAliasSwap(esIndex, indexType)
            .futureAction
            .map(actionRespose => logger.info(s"Completed index swap ${actionRespose}"))
            .recover { case exception: Exception => logger.info(s"Index swap failed ${exception}") }
        }
      case SaveMode.SAVE_TO_DB =>
        import com.gerritforge.analytics.infrastructure.DBSparkWriterImplicits.withDbWriter
        val connectionProperties = new Properties()
        config.driverClassName.foreach(driverClassName => connectionProperties.put("driver", driverClassName))
        df.saveToDb(config.jdbcConnection.get, config.tableName.get, connectionProperties)
          .futureAction
          .map(actionResponse => logger.info(s"Completed view update ${actionResponse}"))
          .recover { case exception: Exception => logger.info(s"View update failed ${exception}") }
      case _ => logger.warn(s"Unrecognised save mode: ${config.saveMode}. Saving data skipped")
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
