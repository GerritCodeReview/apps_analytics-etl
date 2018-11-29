package com.gerritforge.analytics.auditlog.job
import com.gerritforge.analytics.auditlog.model._
import com.gerritforge.analytics.auditlog.spark.sql.udf.SparkExtractors._
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, SparkSession}
import org.elasticsearch.spark.sql._

import scala.language.implicitConversions

object Main extends App with LazyLogging {

  //TODO:
   // Read directory
   // Command line configuration
   // Drop unnecessary fields
   // Hydrate users

  val eventsPath: String = "/Users/syntonyze/dev/gerrit/audit_log"
  val elasticSearchHost = "127.0.0.1"

  implicit val spark: SparkSession = SparkSession
    .builder()
    .appName("Gerrit AuditLog Analytics ETL")
    .config("spark.master", "local")
    .config("spark.es.nodes", elasticSearchHost)
    .config("spark.es.port", 9200)
    .config("es.index.auto.create", "true")
    .getOrCreate()

  import spark.implicits._

  val events = spark
    .read
    .textFile(eventsPath)
    .rdd
    .flatMap(auditString => AuditEvent.parseRaw(auditString).toOption)
    .map(_.toJsonString)
    .toDS()

  implicit private def stringToColumn(name: String): Column = col(name)

  val timestampSecsCol = "time_at_start".divide(1000) // time_at_start is in milliseconds
  val timeBucketCol = "events_time_bucket"
  val commandCol = "command"
  val commandArgsCol = "command_arguments"
  val numEventsCol = "num_events"

  spark.sqlContext.read.json(events)
      .withColumn(timeBucketCol, date_trunc(format="hour", from_unixtime(timestampSecsCol)))
      .withColumn(commandCol, extractCommandUDF("what", "access_path", "http_method"))
      .withColumn(commandArgsCol, extractCommandArgumentsUDF("what", "access_path"))
      .groupBy(timeBucketCol, "audit_type", "who", "access_path", commandCol, commandArgsCol)
      .agg(count("*").alias(numEventsCol))
      .saveToEs("gerrit/auditlog")
}
