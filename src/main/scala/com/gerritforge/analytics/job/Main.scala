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

import java.time.LocalDate

import com.gerritforge.analytics.engine.events.GerritEventsTransformations.NotParsableJsonEvent
import com.gerritforge.analytics.engine.events.{AggregationStrategy, EventParser, GerritJsonEvent}
import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProject, GerritProjectsSupport}
import com.gerritforge.analytics.support.ops.AnalyticsTimeOps
import com.gerritforge.analytics.support.ops.AnalyticsTimeOps.AnalyticsDateTimeFormater
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import scopt.Read.reads
import scopt.{OptionParser, Read}

import scala.io.{Codec, Source}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object Main extends App with Job with LazyLogging with FetchRemoteProjects {

  private val fileExists: String => Either[String, Unit] = { path =>
    if (!new java.io.File(path).exists) {
      Left(s"ERROR: Path '$path' doesn't exists!")
    } else {
      Right()
    }
  }

  implicit val localDateRead: Read[LocalDate] = reads { dateStr =>
    val cliDateFormat = AnalyticsDateTimeFormater.yyyy_MM_dd
    try {
      import AnalyticsTimeOps.implicits._
      dateStr.parseStringToLocalDate(cliDateFormat).get
    } catch {
      case NonFatal(e) =>
        throw new IllegalArgumentException(s"Invalid date '$dateStr' expected format is '${cliDateFormat}'")
    }
  }

  private val cliOptionParser: OptionParser[GerritEndpointConfig] = new scopt.OptionParser[GerritEndpointConfig]("scopt") {
    head("scopt", "3.x")
    opt[String]('u', "url") optional() action { (x, c) =>
      c.copy(baseUrl = Some(x))
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
    opt[LocalDate]('s', "since") optional() action { (x, c) =>
      c.copy(since = Some(x))
    } text "begin date "
    opt[LocalDate]('u', "until") optional() action { (x, c) =>
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
    } text "location where to load the Gerrit Events"
    opt[String]("writeNotProcessedEventsTo") optional() action { (failedEventsPath, config) =>
      config.copy(eventsFailureOutputPath = Some(failedEventsPath))
    } text "location where to write a TSV file containing the events we couldn't process with a description fo the reason why"
  }

  cliOptionParser.parse(args, GerritEndpointConfig()) match {
    case Some(config) =>
      implicit val spark: SparkSession = SparkSession.builder()
        .appName("Gerrit Analytics ETL")
        .getOrCreate()

      implicit val _: GerritEndpointConfig = config

      logger.info(s"Starting analytics app with config $config")

      val dataFrame = buildProjectStats().cache() //This dataframe is written twice

      logger.info(s"ES content created, saving it to '${config.outputDir}'")
      dataFrame.write.json(config.outputDir)

      saveES(dataFrame)

    case None => // invalid configuration usage has been displayed
  }
}

trait Job { self: LazyLogging with FetchProjects =>
  implicit val codec = Codec.ISO8859

  def buildProjectStats()(implicit config: GerritEndpointConfig, spark: SparkSession): DataFrame = {
    import com.gerritforge.analytics.engine.GerritAnalyticsTransformations._
    import com.gerritforge.analytics.engine.events.GerritEventsTransformations._

    implicit val sc: SparkContext = spark.sparkContext

    val aggregationStrategy: AggregationStrategy =
      config.aggregate.flatMap { agg =>
        AggregationStrategy.byName(agg) match {
          case Success(strategy) => Some(strategy)
          case Failure(e) =>
            logger.warn(s"Invalid aggregation strategy '$agg", e)
            None
        }
      }.getOrElse(AggregationStrategy.aggregateByEmail)

    val projects = fetchProjects(config)

    logger.info(s"Loaded a list of ${projects.size} projects ${if (projects.size > 20) projects.take(20).mkString("[", ",", ", ...]") else projects.mkString("[", ",", "]")}")

    val aliasesDF = getAliasDF(config.emailAlias)

    val events = loadEvents

    val failedEvents: RDD[NotParsableJsonEvent] = events.collect { case Left(eventFailureDescription) => eventFailureDescription }

    if (!failedEvents.isEmpty()) {
      config.eventsFailureOutputPath.foreach { failurePath =>
        logger.info(s"Events failures will be stored at '$failurePath'")

        import spark.implicits._
        failedEvents.toDF().write.option("sep", "\t").option("header", true).csv(failurePath)
      }
    }

    //We might want to use the time of the events as information to feed to the collection of data from the repository
    val repositoryAlteringEvents = events.collect { case Right(event) => event }.repositoryWithNewRevisionEvents

    val firstEventDateMaybe: Option[LocalDate] = if (repositoryAlteringEvents.isEmpty()) None else Some(repositoryAlteringEvents.earliestEventTime.toLocalDate)

    val configWithOverriddenUntil = firstEventDateMaybe.fold(config) { firstEventDate =>
      val lastAggregationDate = firstEventDate.plusMonths(1)
      if (lastAggregationDate.isBefore(LocalDate.now())) {
        logger.info(s"Overriding 'until' date '${config.until}' with '$lastAggregationDate' since events ara available until $firstEventDate")
        config.copy(until = Some(lastAggregationDate))
      } else {
        config
      }
    }

    val statsFromAnalyticsPlugin =
      getContributorStatsFromAnalyticsPlugin(spark.sparkContext.parallelize(projects), configWithOverriddenUntil.contributorsUrl)

    val statsFromEvents = getContributorStatsFromGerritEvents(repositoryAlteringEvents, statsFromAnalyticsPlugin.commitSet.rdd, aggregationStrategy)

    val mergedEvents = if (statsFromEvents.head(1).isEmpty) {
      statsFromAnalyticsPlugin
    } else {
      require(statsFromAnalyticsPlugin.schema == statsFromEvents.schema,
        s""" Schemas from the stats collected from events and from the analytics datasets differs!!
           | From analytics plugin: ${statsFromAnalyticsPlugin.schema}
           | From gerrit events: ${statsFromEvents.schema}
      """.stripMargin)

      (statsFromAnalyticsPlugin union statsFromEvents)
    }

    mergedEvents.dashboardStats(aliasesDF)
  }

  def loadEvents(implicit config: GerritEndpointConfig, spark: SparkSession): RDD[Either[NotParsableJsonEvent, GerritJsonEvent]] = { // toDF
    import com.gerritforge.analytics.engine.events.GerritEventsTransformations._

    config.eventsPath.fold(spark.sparkContext.emptyRDD[Either[NotParsableJsonEvent, GerritJsonEvent]]) { eventsPath =>
      spark
        .read.textFile(eventsPath).rdd
        .parseEvents(EventParser)
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

trait FetchProjects {
  def fetchProjects(config: GerritEndpointConfig): Seq[GerritProject]
}

trait FetchRemoteProjects extends FetchProjects {

  def fetchProjects(config: GerritEndpointConfig): Seq[GerritProject] = {
    config.gerritProjectsUrl.toSeq.flatMap { url => GerritProjectsSupport.parseJsonProjectListResponse(Source.fromURL(url)) }
  }
}

