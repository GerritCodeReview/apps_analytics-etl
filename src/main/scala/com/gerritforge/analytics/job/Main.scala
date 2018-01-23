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

import com.gerritforge.analytics.engine.events.{AggregationStrategy, EventParser, GerritJsonEvent, GerritRefHasNewRevisionEvent}
import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProject, GerritProjectsRDD}
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import scopt.OptionParser

import scala.io.{Codec, Source}

object Main extends App with Job with LazyLogging {

  private val fileExists: String => Either[String, Unit] = { path =>
    if (!new java.io.File(path).exists) {
      Left(s"ERROR: Path '$path' doesn't exists!")
    } else {
      Right()
    }
  }

  private val cliOptionParser: OptionParser[GerritEndpointConfig] = new scopt.OptionParser[GerritEndpointConfig]("scopt") {
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
    } text "output directory"
    opt[String]('s', "since") optional() action { (x, c) =>
      c.copy(since = Some(x))
    } text "begin date "
    opt[String]('u', "until") optional() action { (x, c) =>
      c.copy(until = Some(x))
    } text "since date"
    opt[String]('g', "aggregate") optional() action { (x, c) =>
      c.copy(aggregate = Some(x))
    } text "aggregate email/email_hour/email_day/email_month/email_year"

    opt[String]('a', "email-aliases") optional() validate fileExists action { (path, c) =>
      c.copy(emailAlias = Some(path))
    } text "\"emails to author alias\" input data path"
    opt[String]("events") optional() action { (eventsPath, config) =>
      config.copy(eventsPath = Some(eventsPath))
    }
  }

  cliOptionParser.parse(args, GerritEndpointConfig()) match {
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
    import com.gerritforge.analytics.engine.GerritAnalyticsTransformations._
    import com.gerritforge.analytics.engine.events.GerritEventsTransformations._

    implicit val sc: SparkContext = spark.sparkContext

    val aggregationStrategy: AggregationStrategy =
      config.aggregate.map(AggregationStrategy.byName).getOrElse(AggregationStrategy.aggregateByEmail)

    val projects = GerritProjectsRDD(Source.fromURL(config.gerritProjectsUrl))

    val aliasesDF = getAliasDF(config.emailAlias)

    //We might want to use the time of the events as information to feed to the collection of data from the repository
    val allEvents  = loadEvents.repositoryWithNewRevisionEvents

    val statsFromAnalyticsPlugin = getContributorStatsFromAnalyticsPlugin(projects, config.contributorsUrl)

    val statsFromEvents = getContributorStatsFromGerritEvents(allEvents, statsFromAnalyticsPlugin.commitSet.rdd, aggregationStrategy)

    require(statsFromAnalyticsPlugin.schema == statsFromEvents.schema,
      s""" Schemas from the stats collected from events and from the analytics datasets differs!!
        | From analytics plugin: ${statsFromAnalyticsPlugin.schema}
        | From gerrit events: ${statsFromEvents.schema}
      """.stripMargin)

    (statsFromAnalyticsPlugin union statsFromEvents).dashboardStats(aliasesDF)
  }

  def loadEvents(implicit config: GerritEndpointConfig, spark: SparkSession): RDD[GerritJsonEvent] = { // toDF
    import com.gerritforge.analytics.engine.events.GerritEventsTransformations._

    config.eventsPath.fold(spark.sparkContext.emptyRDD[GerritJsonEvent]) { eventsPath =>
      spark
        .read.textFile(eventsPath).rdd
        .parseEvents(EventParser)
        //We should handle the Left cases with some diagnostics
        .collect { case Right(event) =>
        event
      }
    }
  }

  def saveES(df: DataFrame)(implicit config: GerritEndpointConfig) {
    import org.elasticsearch.spark.sql._
    config.elasticIndex.foreach { esIndex =>
      logger.info(s"ES content created, saving it to elastic search instance at '${config.elasticIndex}'")

      df.saveToEs(esIndex)
    }

  }
}

