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
import com.gerritforge.analytics.gitcommits.model.{ GerritProject, GerritProjectsSupport}
import com.gerritforge.analytics.spark.SparkApp
import com.gerritforge.analytics.support.ops.ReadsOps._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import scopt.OptionParser
import com.gerritforge.analytics.gitcommits.model.GerritEndpointConfig._

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

  private val cliOptionParser: OptionParser[Map[String, _]] =
    new scopt.OptionParser[Map[String, _]]("scopt") {
      head("scopt", "3.x")
      opt[String]('u', "url") optional () action { (x, c) =>
        c+(BASE_URL -> x)
      } text "gerrit url"
      opt[String]('p', "prefix") optional () action { (p, c) =>
        c + (PREFIX -> p)
      } text "projects prefix"
      opt[String]('o', "out") optional () action { (x, c) =>
        c + (OUTPUT_DIR -> x)
      } text "output directory"
      cmd("saveToEs").optional()
        .action((_, c) => c + (SAVE_MODE -> SaveMode.SAVE_TO_ES.toString))
        .children(
          opt[String]('e', "elasticSearchIndex") required() action { (input, c) =>
            c + (ELASTIC_SEARCH_INDEX -> input)
          } text "elasticSearch index to persist data into (Required)"

        )
      cmd("saveToDb").optional()
        .action((_, c) => c + (SAVE_MODE -> SaveMode.SAVE_TO_DB.toString))
        .children(
          opt[String]('j', "jdbcConnection") required() action { (input, c) =>
            c + (RELATIONAL_DATABASE_JDBC_CONNECTION -> input)
          } text "Jdbc connection string",
          opt[String]('t', "tableName") required() action { (input, c) =>
            c + (RELATIONAL_DATABASE_TABLE -> input)
          } text "Database table name",
          opt[String]('d', "driverClass") optional() action { (input, c) =>
            c + (RELATIONAL_DATABASE_DRIVER -> input)
          } text "Database jdbc driver class name"
        )
      opt[LocalDate]('s', "since") optional () action { (x, c) =>
        c + (SINCE -> x.toEpochDay)
      } text "begin date "
      opt[LocalDate]('u', "until") optional () action { (x, c) =>
        c + (UNTIL -> x.toEpochDay)
      } text "since date"
      opt[String]('g', "aggregate") optional () action { (x, c) =>
        c + (AGGREGATE -> x)
      } text "aggregate email/email_hour/email_day/email_month/email_year"

      opt[String]('a', "email-aliases") optional () validate fileExists action { (path, c) =>
        c + (EMAIL_ALIAS -> path)
      } text "\"emails to author alias\" input data path"

      opt[String]("username") optional () action { (input, c) =>
        c + (USERNAME -> input)
      } text "Gerrit API Username"

      opt[String]("password") optional () action { (input, c) =>
        c + (PASSWORD -> input)
      } text "Gerrit API Password"

      opt[Boolean]('k', "ignore-ssl-cert") optional () action { (input, c) =>
        c + (IGNORE_SSL_CERT -> input)
      } text "Ignore SSL certificate validation"

      opt[Boolean]('r', "extract-branches") optional () action { (input, c) =>
        c + (EXTRACT_BRANCHES -> input)
      } text "enables branches extraction for each commit"
      checkConfig(config => if(config.get(ELASTIC_SEARCH_INDEX).isEmpty && config.get(RELATIONAL_DATABASE_JDBC_CONNECTION).isEmpty) Left("saveToEs or saveToDb command must be specified") else Right())
    }

  cliOptionParser.parse(args, Map()) match {
    case Some(c) =>
      import collection.JavaConverters._
      val config = ConfigFactory.parseMap(c.asJava)
      implicit val _: Config = config

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

  def buildProjectStats()(implicit config: Config, spark: SparkSession): DataFrame = {
    import com.gerritforge.analytics.gitcommits.engine.GerritAnalyticsTransformations._

    implicit val sc: SparkContext = spark.sparkContext
    val projects = fetchProjects(config)

    logger.info(
      s"Loaded a list of ${projects.size} projects ${if (projects.size > 20) projects.take(20).mkString("[", ",", ", ...]")
      else projects.mkString("[", ",", "]")}")

    val aliasesDF = getAliasDF(config.getOptional[String](EMAIL_ALIAS))

    val contributorsStats = getContributorStats(spark.sparkContext.parallelize(projects),
                                             config.contributorsUrl,
                                             config.gerritApiConnection)
    contributorsStats.dashboardStats(aliasesDF)
  }

  def save(df: DataFrame)(implicit config: Config) {
    import scala.concurrent.ExecutionContext.Implicits.global
    SaveMode.withName(config.getString(SAVE_MODE)) match {
      case SaveMode.SAVE_TO_ES =>
        val esIndex = config.getString(ELASTIC_SEARCH_INDEX)
        import com.gerritforge.analytics.infrastructure.ESSparkWriterImplicits.withAliasSwap
        df.saveToEsWithAliasSwap(esIndex, indexType)
            .futureAction
            .map(actionRespose => logger.info(s"Completed index swap ${actionRespose}"))
            .recover { case exception: Exception => logger.info(s"Index swap failed ${exception}") }

      case SaveMode.SAVE_TO_DB =>
        import com.gerritforge.analytics.infrastructure.DBSparkWriterImplicits.withDbWriter
        val connectionProperties = new Properties()
        config.getOptional[String](RELATIONAL_DATABASE_DRIVER)
          .foreach(driverClassName =>
            connectionProperties.put("driver", driverClassName))

       df.saveToDb(
         config.getString(RELATIONAL_DATABASE_JDBC_CONNECTION),
         config.getString(RELATIONAL_DATABASE_TABLE),
         connectionProperties)
          .futureAction
          .map(actionResponse => logger.info(s"Completed view update ${actionResponse}"))
          .recover { case exception: Exception => logger.info(s"View update failed ${exception}") }
      case saveMode => logger.warn(s"Unrecognised save mode: $saveMode. Saving data skipped")
    }

  }
}

trait FetchProjects {
  def fetchProjects(config: Config): Seq[GerritProject]
}

trait FetchRemoteProjects extends FetchProjects {
  def fetchProjects(config: Config): Seq[GerritProject] = {

    config.gerritProjectsUrl.toSeq.flatMap { url =>
      GerritProjectsSupport.parseJsonProjectListResponse(
        config.gerritApiConnection.getContentFromApi(url))
    }
  }
}
