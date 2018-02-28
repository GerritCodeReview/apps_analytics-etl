package com.gerritforge.analytics.plugin

import java.sql.Timestamp
import java.time.LocalDate

import com.gerritforge.analytics.engine.events.AggregationStrategy
import com.gerritforge.analytics.job.{FetchProjects, Job}
import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProject, GerritProjectsSupport}
import com.google.gerrit.server.project.ProjectControl
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SparkSession}
import org.kohsuke.args4j.{Argument, Option => ArgOption}

import scala.io.Source
import scala.util.{Failure, Success}

@CommandMetaData(name = "start", description = "Start the extraction of Gerrit analytics")
class StartCommand @Inject()(implicit val gerritProjects: GerritProjectsSupport, val gerritConfig: GerritConfigSupport)
  extends SshCommand with Job with DateConversions with FetchProjects with LazyLogging {

  @Argument(index = 0, required = true, metaVar = "PROJECT", usage = "project name")
  var projectControl: ProjectControl = null

  @ArgOption(name = "--elasticIndex", aliases = Array("-e"),
    usage = "index name")
  var elasticIndex: String = "gerrit/analytics"

  @ArgOption(name = "--since", aliases = Array("-s"), usage = "begin date")
  var beginDate: Timestamp = NO_TIMESTAMP

  @ArgOption(name = "--until", aliases = Array("-u"), usage = "end date")
  var endDate: Timestamp = NO_TIMESTAMP

  @ArgOption(name = "--aggregate", aliases = Array("-g"), usage = "aggregate email/email_hour/email_day/email_month/email_year")
  var aggregate: String = "email_day"

  @ArgOption(name = "--email-aliases", aliases = Array("-a"), usage = "\"emails to author alias\" input data path")
  var emailAlias: String = null

  override def run() {
    implicit val config = GerritEndpointConfig(gerritConfig.getListenUrl().get, prefix = Option(projectControl).map(_.getProject.getName), "", elasticIndex,
      beginDate, endDate, aggregate, emailAlias)

    implicit val spark: SparkSession = SparkSession.builder()
      .appName("analytics-etl")
      .master("local")
      .config("key", "value")
      .getOrCreate()

    implicit lazy val sc: SparkContext = spark.sparkContext
    implicit lazy val sql: SQLContext = spark.sqlContext

    val prevClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
    try {
      stdout.println(s"Starting new Spark job with parameters: $config")
      stdout.flush()
      val startTs = System.currentTimeMillis
      val projectStats = buildProjectStats().cache()
      val numRows = projectStats.count()

        import org.elasticsearch.spark.sql._
        config.elasticIndex.foreach { esIndex =>
          stdout.println(s"$numRows rows extracted. Posting Elasticsearch at '${config.elasticIndex}'")
          stdout.flush()
          projectStats.saveToEs(esIndex)
        }

      val elaspsedTs = (System.currentTimeMillis - startTs) / 1000L
      stdout.println(s"Job COMPLETED in $elaspsedTs secs")
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        stderr.println(s"Job FAILED: ${e.getClass} : ${e.getMessage}")
        die(e)
    } finally {
      Thread.currentThread().setContextClassLoader(prevClassLoader)
    }
  }

  def fetchProjects(config: GerritEndpointConfig): Seq[GerritProject] = {
    config.prefix.toSeq.flatMap(projectName => gerritProjects.getProject(projectName) match {
      case Success(project) =>
        Seq(project)
      case Failure(e) => {
        logger.warn(s"Unable to fetch project $projectName", e)
        Seq()
      }
    })
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


