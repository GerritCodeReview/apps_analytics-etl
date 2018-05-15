package com.gerritforge.analytics.plugin

import java.sql.Timestamp
import java.time.LocalDate

import com.gerritforge.analytics.engine.events.AggregationStrategy
import com.gerritforge.analytics.job.Job
import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProjectsSupport}
import com.google.gerrit.server.project.ProjectControl
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.kohsuke.args4j.{Argument, Option => ArgOption}

import scala.util.{Failure, Success}

@CommandMetaData(name = "start", description = "Start the extraction of Gerrit analytics")
class StartCommand @Inject() (val gerritProjectsSupport: GerritProjectsSupport)
  extends SshCommand with Job with DateConversions with LazyLogging {

  @Argument(index = 0, required = true, metaVar = "PROJECT", usage = "project name")
  var projectControl: ProjectControl = null

  @ArgOption(name = "--elasticIndex", aliases = Array("-e"),
    usage = "index name")
  var elasticIndex: String = null

  @ArgOption(name = "--since", aliases = Array("-s"), usage = "begin date")
  var beginDate: Timestamp = NO_TIMESTAMP

  @ArgOption(name = "--until", aliases = Array("-u"), usage = "end date")
  var endDate: Timestamp = NO_TIMESTAMP

  @ArgOption(name = "--aggregate", aliases = Array("-g"), usage = "aggregate email/email_hour/email_day/email_month/email_year")
  var aggregate: String = null

  @ArgOption(name = "--email-aliases", aliases = Array("-a"), usage = "\"emails to author alias\" input data path")
  var emailAlias: String = null



  override def run() {
    implicit val config = GerritEndpointConfig(None, prefix = Option(projectControl).map(_.getProject.getName), "", elasticIndex,
      beginDate, endDate, aggregate, emailAlias)

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

    val prevClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
    try {
      val projectStats = buildProjectStats()

      projectStats.collect() foreach stdout.println
    } finally {
      Thread.currentThread().setContextClassLoader(prevClassLoader)
    }
  }
}

trait DateConversions {
  val NO_TIMESTAMP = new Timestamp(0L)

  implicit def timestampToLocalDate(timestamp: Timestamp): Option[LocalDate] = timestamp match {
    case NO_TIMESTAMP => None
    case ts => Some(ts.toLocalDateTime.toLocalDate)
  }

  implicit def nullableStringToOption(nullableString: String): Option[String] = Option(nullableString)
}

