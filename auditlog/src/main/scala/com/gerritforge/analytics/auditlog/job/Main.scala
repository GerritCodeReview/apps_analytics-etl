package com.gerritforge.analytics.auditlog.job

import com.gerritforge.analytics.auditlog.broadcast.GerritAccounts
import com.gerritforge.analytics.auditlog.model.ElasticSearchFields._
import com.gerritforge.analytics.auditlog.model._
import com.gerritforge.analytics.auditlog.range.TimeRange
import com.gerritforge.analytics.auditlog.spark.dataframe.ops.DataFrameOps._
import com.gerritforge.analytics.auditlog.spark.rdd.ops.SparkRDDOps._
import com.gerritforge.analytics.auditlog.spark.session.ops.SparkSessionOps._
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.SparkSession
import org.elasticsearch.spark.sql._

object Main extends App with LazyLogging {

  CommandLineArguments(args) match {
    case Some(config) =>
      val tryGerritAccounts = GerritAccounts.loadAccounts(
        new GerritConnectivity(config.gerritUsername, config.gerritPassword, config.ignoreSSLCert.getOrElse(false)),
        config.gerritUrl.get
      )

      if(tryGerritAccounts.isFailure) {
        logger.error("Error loading gerrit accounts", tryGerritAccounts.failed.get)
        sys.exit(1)
      }

      implicit val spark: SparkSession = SparkSession
        .builder()
        .appName("Gerrit AuditLog Analytics ETL")
        .getOrCreate()

      val gerritAccounts = spark.sparkContext.broadcast(tryGerritAccounts.get)

      spark
        .getEventsFromPath(config.eventsPath.get)
        .filterWithinRange(TimeRange(config.since, config.until))
        .toJsonString
        .toJsonTableDataFrame
        .hydrateWithUserIdentifierColumn(USER_IDENTIFIER_FIELD, gerritAccounts.value)
        .withTimeBucketColum(TIME_BUCKET_FIELD, config.eventsTimeAggregation.get)
        .withCommandColumns(COMMAND_FIELD, COMMAND_ARGS_FIELD)
        .aggregateNumEventsColumn(NUM_EVENTS_FIELD, FACETING_FIELDS)
        .saveToEs(s"${config.elasticSearchIndex.get}/$DOCUMENT_TYPE")

    case None =>
      logger.error("Could not parse command line arguments")
      sys.exit(1)
  }
}
