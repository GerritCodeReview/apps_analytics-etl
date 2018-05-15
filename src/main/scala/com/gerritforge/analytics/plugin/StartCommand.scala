package com.gerritforge.analytics.plugin

import java.net.{URL, URLClassLoader}
import java.time.LocalDate

import com.gerritforge.analytics.engine.events.AggregationStrategy
import com.gerritforge.analytics.engine.events.GerritEventsTransformations.logger
import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProjectsSupport}
import com.google.gerrit.extensions.api.GerritApi
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import org.apache.spark.SparkContext
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.kohsuke.args4j.{Option => ArgOption}
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source
import scala.util.{Failure, Success}

@CommandMetaData(name = "start", description = "Start the extraction of Gerrit analytics")
class StartCommand @Inject() (val gerritProjectsSupport: GerritProjectsSupport)
  extends SshCommand {
  val logger: Logger = LoggerFactory.getLogger(classOf[StartCommand])

  @ArgOption(name = "--prefix", aliases = Array("-p"),
    usage = "projects prefix")
  var prefix: Option[String] = None

  @ArgOption(name = "--elasticIndex", aliases = Array("-e"),
    usage = "index name")
  var elasticIndex: Option[String] = None

  @ArgOption(name = "--since", aliases = Array("-s"), usage = "begin date")
  var beginDate: Option[LocalDate] = None

  @ArgOption(name = "--until", aliases = Array("-u"), usage = "end date")
  var endDate: Option[LocalDate] = None

  @ArgOption(name = "--aggregate", aliases = Array("-g"), usage = "aggregate email/email_hour/email_day/email_month/email_year")
  var aggregate: Option[String] = None

  @ArgOption(name = "--email-aliases", aliases = Array("-a"), usage = "\"emails to author alias\" input data path")
  var emailAlias: Option[String] = None

  def getConfig() = GerritEndpointConfig("", prefix,"", elasticIndex,
      beginDate, endDate, aggregate, emailAlias)

  override def run() {
    val config = getConfig()

    implicit val spark: SparkSession = SparkSession.builder()
      .appName("analytics-etl")
      .master("local")
      .config("key", "value")
      .getOrCreate()

    implicit lazy val sc: SparkContext = spark.sparkContext
    implicit lazy val sql: SQLContext = spark.sqlContext

    val aggregationStrategy: AggregationStrategy =
      config.aggregate.flatMap { agg =>
        AggregationStrategy.byName(agg) match {
          case Success(strategy) => Some(strategy)
          case Failure(e) =>
            logger.warn(s"Invalid aggregation strategy '$agg", e)
            None
        }
      }.getOrElse(AggregationStrategy.aggregateByEmail)

    val projects = gerritProjectsSupport.getProjectList(prefix)

    stdout.println(s"Projects: $projects")
  }
}

