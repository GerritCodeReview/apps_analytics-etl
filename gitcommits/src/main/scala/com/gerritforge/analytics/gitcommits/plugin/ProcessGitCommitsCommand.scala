package com.gerritforge.analytics.gitcommits.plugin

import java.sql.Timestamp
import java.time.LocalDate

import com.gerritforge.analytics.gitcommits.job.{FetchProjects, Job}
import com.gerritforge.analytics.gitcommits.model.{
  GerritEndpointConfig,
  GerritProjectWithRef,
  GerritProjectsSupport
}
import com.google.gerrit.server.project.ProjectControl
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.kohsuke.args4j.{Argument, Option => ArgOption}

import scala.util.{Failure, Success}

@CommandMetaData(
  name = "processGitCommits",
  description = "Start the extraction of Git Commits Gerrit analytics"
)
class ProcessGitCommitsCommand @Inject()(
    implicit val gerritProjects: GerritProjectsSupport,
    val gerritConfig: GerritConfigSupport
) extends SshCommand
    with Job
    with DateConversions
    with FetchProjects
    with LazyLogging {

  @Argument(index = 0, required = true, metaVar = "PROJECT", usage = "project name")
  var projectControl: ProjectControl = null

  @ArgOption(name = "--elasticIndex", aliases = Array("-e"), usage = "index name")
  var elasticIndex: String = "gerrit"

  @ArgOption(name = "--since", aliases = Array("-s"), usage = "begin date")
  var beginDate: Timestamp = NO_TIMESTAMP

  @ArgOption(name = "--until", aliases = Array("-u"), usage = "end date")
  var endDate: Timestamp = NO_TIMESTAMP

  @ArgOption(
    name = "--aggregate",
    aliases = Array("-g"),
    usage = "aggregate email/email_hour/email_day/email_month/email_year"
  )
  var aggregate: String = "email_day"

  @ArgOption(
    name = "--email-aliases",
    aliases = Array("-a"),
    usage = "\"emails to author alias\" input data path"
  )
  var emailAlias: String = null

  @ArgOption(
    name = "--ignore-ssl-cert",
    aliases = Array("-k"),
    usage = "Ignore SSL certificate validation"
  )
  var ignoreSSLCert: Boolean = false

  @ArgOption(
    name = "--extract-branches",
    aliases = Array("-r"),
    usage = "enables branches extraction for each commit"
  )
  var extractBranches: Boolean = false

  @ArgOption(
    name = "--manifest",
    aliases = Array("-m"),
    usage = "repo manifest XML with the list of projects to process"
  )
  var manifest: String = null

  @ArgOption(
    name = "--manifest-branch",
    aliases = Array("-n"),
    usage = "manifest file git branch"
  )
  var manifestBranch: String = null

  @ArgOption(
    name = "--manifest-label",
    aliases = Array("-l"),
    usage = "A 'manifest_label' is an aggregation of projects imported from the same manifest. Add it to allow filtering by 'manifest_label'"
  )
  var manifestLabel: String = null

  override def run() {
    implicit val config = GerritEndpointConfig(
      gerritConfig.getListenUrl(),
      prefix = Option(projectControl).map(_.getProject.getName),
      "",
      elasticIndex,
      beginDate,
      endDate,
      aggregate,
      emailAlias,
      ignoreSSLCert = Some(ignoreSSLCert)
    )

    implicit val spark: SparkSession = SparkSession
      .builder()
      .appName("analytics-etl")
      .master("local")
      .config("key", "value")
      .getOrCreate()

    implicit lazy val sc: SparkContext = spark.sparkContext
    implicit lazy val sql: SQLContext  = spark.sqlContext

    val prevClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
    try {
      stdout.println(s"Starting new Spark job with parameters: $config")
      stdout.flush()
      val startTs      = System.currentTimeMillis
      val projectStats = buildProjectStats().cache()
      val numRows      = projectStats.count()

      import org.elasticsearch.spark.sql._
      config.elasticIndex.foreach { esIndex =>
        stdout.println(
          s"$numRows rows extracted. Posting Elasticsearch at '${config.elasticIndex}'"
        )
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

  def fetchProjects(config: GerritEndpointConfig): Seq[GerritProjectWithRef] = {
    config.prefix.toSeq.flatMap(
      projectName =>
        gerritProjects.getProject(projectName) match {
          case Success(project) =>
            Seq(project)
          case Failure(e) => {
            logger.warn(s"Unable to fetch project $projectName", e)
            Seq()
          }
        }
    )
  }
}

trait DateConversions {
  val NO_TIMESTAMP = new Timestamp(0L)

  implicit def timestampToLocalDate(timestamp: Timestamp): Option[LocalDate] = timestamp match {
    case NO_TIMESTAMP => None
    case ts           => Some(ts.toLocalDateTime.toLocalDate)
  }

  implicit def nullableStringToOption(nullableString: String): Option[String] =
    Option(nullableString)
}
