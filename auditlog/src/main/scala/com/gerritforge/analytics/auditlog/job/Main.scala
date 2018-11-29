package com.gerritforge.analytics.auditlog.job
import java.time.LocalDate

import com.gerritforge.analytics.auditlog.broadcast.GerritAccounts
import com.gerritforge.analytics.auditlog.model._
import com.gerritforge.analytics.auditlog.spark.sql.udf.SparkExtractors._
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, SparkSession}
import com.gerritforge.analytics.support.ops.ReadsOps._
import org.elasticsearch.spark.sql._
import scopt.OptionParser

import scala.language.implicitConversions

object Main extends App with LazyLogging {

  //TODO:
   // Drop unnecessary fields
   // Filter by date

  Args.cliOptionParser.parse(args, AuditLogETLConfig()) match {
    case Some(config) =>
      val tryGerritAccounts = GerritAccounts.loadAccounts(
        new GerritConnectivity(config.gerritUsername, config.gerritPassword, config.ignoreSSLCert.getOrElse(false)),
        config.gerritUrl.get
      )

      if(tryGerritAccounts.isFailure) {
        println("Error loading gerrit accounts", tryGerritAccounts.failed.get)
        logger.error("Error loading gerrit accounts", tryGerritAccounts.failed.get)
        sys.exit(1)
      }

      val spark: SparkSession = SparkSession
        .builder()
        .appName("Gerrit AuditLog Analytics ETL")
        .getOrCreate()

      import spark.implicits._
      val gerritAccounts = spark.sparkContext.broadcast(tryGerritAccounts.get)

      val eventsDS = spark
        .read
        .textFile(config.eventsDirectoryPath.get)
        .rdd
        .flatMap(auditString => AuditEvent.parseRaw(auditString).toOption)
        .map(_.toJsonString)
        .toDS()

      implicit def stringToColumn(name: String): Column = col(name)

      val timestampSecsCol = "time_at_start".divide(1000) // time_at_start is in milliseconds
      val timeBucketCol = "events_time_bucket"
      val commandCol = "command"
      val commandArgsCol = "command_arguments"
      val numEventsCol = "num_events"
      val userIdentifierCol = "user_identifier"

      def extractIdentifier: UserDefinedFunction = udf((who: Int) => gerritAccounts.value.getIdentifier(who))

      spark.sqlContext.read.json(eventsDS)
        .withColumn(timeBucketCol, date_trunc(format="hour", from_unixtime(timestampSecsCol)))
        .withColumn(commandCol, extractCommandUDF("what", "access_path", "http_method"))
        .withColumn(commandArgsCol, extractCommandArgumentsUDF("what", "access_path"))
        .withColumn(userIdentifierCol, extractIdentifier("who"))
        .groupBy(timeBucketCol, "audit_type", userIdentifierCol, "access_path", commandCol, commandArgsCol)
        .agg(count("*").alias(numEventsCol))
          .saveToEs(s"${config.elasticSearchIndex.get}/auditlog")

    case None => sys.exit(1)
  }
}

object Args {
  val cliOptionParser: OptionParser[AuditLogETLConfig] =
    new scopt.OptionParser[AuditLogETLConfig]("spark-submit") {
      head("spark-submit")
      opt[String]('u', "gerritUrl") required() action { (input, c) =>
        c.copy(gerritUrl = Some(input))
      } text "gerrit URL"

      opt[String]('i', "elasticSearchIndex") required() action { (input, c) =>
        c.copy(elasticSearchIndex = Some(input))
      } text "elasticSearch URL"

      opt[String]('p',"eventsDirectoryPath") required () action { (input, c) =>
        c.copy(eventsDirectoryPath = Some(input))
      } text "path to a directory or a file containing auditlogs events"

      opt[String]("username") optional () action { (input, c) =>
        c.copy(gerritUsername = Some(input))
      } text "Gerrit API Username"

      opt[String]("password") optional () action { (input, c) =>
        c.copy(gerritPassword = Some(input))
      } text "Gerrit API Password"

      opt[Boolean]('k', "ignoreSSLCert") optional () action { (input, c) =>
        c.copy(ignoreSSLCert = Some(input))
      } text "Ignore SSL certificate validation"

      opt[LocalDate]('s', "since") optional () action { (input, c) =>
        c.copy(since = Some(input))
      } text "begin date "

      opt[LocalDate]('u', "until") optional () action { (input, c) =>
        c.copy(until = Some(input))
      } text "until date"
    }
}
