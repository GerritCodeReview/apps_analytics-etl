package com.gerritforge.analytics.auditlog.spark.dataframe.ops

import com.gerritforge.analytics.auditlog.broadcast.GerritUserIdentifiers
import com.gerritforge.analytics.auditlog.spark.sql.udf.SparkExtractors.{extractCommandArgumentsUDF, extractCommandUDF}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{udf, _}


object DataFrameOps {

  implicit class PimpedAuditLogDataFrame(dataFrame: DataFrame) {
    def hydrateWithUserIdentifierColumn(userIdentifierCol: String, gerritAccounts: GerritUserIdentifiers): DataFrame = {
      def extractIdentifier: UserDefinedFunction = udf((who: Int) => gerritAccounts.getIdentifier(who))

        dataFrame.withColumn(userIdentifierCol, extractIdentifier(col("who")))
    }

    def withTimeBucketColum(timeBucketCol: String, timeAggregation: String): DataFrame = {
      dataFrame
        .withColumn(timeBucketCol, date_trunc(format=timeAggregation, from_unixtime(col("time_at_start").divide(1000))))
    }

    def withCommandColumns(commandCol: String, commandArgsCol: String): DataFrame = {
      dataFrame
        .withColumn(commandCol, extractCommandUDF(col("what"), col("access_path"), col("http_method")))
        .withColumn(commandArgsCol, extractCommandArgumentsUDF(col("what"), col("access_path")))
    }

    def aggregateNumEventsColumn(numEventsCol: String, cols: List[String]): DataFrame = {
      dataFrame.groupBy(cols.map(c => col(c)): _*)
        .agg(count("*")
        .alias(numEventsCol))
    }
  }
}
