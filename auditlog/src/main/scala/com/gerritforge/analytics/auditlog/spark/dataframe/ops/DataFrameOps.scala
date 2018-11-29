// Copyright (C) 2018 GerritForge Ltd
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

package com.gerritforge.analytics.auditlog.spark.dataframe.ops

import com.gerritforge.analytics.auditlog.broadcast.GerritUserIdentifiers
import com.gerritforge.analytics.auditlog.spark.sql.udf.SparkExtractors.{extractCommandArgumentsUDF, extractCommandUDF}
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.{udf, _}

import scala.util.Try


object DataFrameOps {

  implicit class PimpedAuditLogDataFrame(dataFrame: DataFrame) {

    private def hasColumn(path: String) = Try(dataFrame(path)).isSuccess

    private def ifExistThenGetOrNull(column: String, targetColumn: => Column) = if (hasColumn(column)) targetColumn else lit(null)

    def hydrateWithUserIdentifierColumn(userIdentifierCol: String, gerritAccounts: GerritUserIdentifiers): DataFrame = {
      def extractIdentifier: UserDefinedFunction = udf((who: Int) => gerritAccounts.getIdentifier(who))

      dataFrame.withColumn(userIdentifierCol, ifExistThenGetOrNull("who", extractIdentifier(col("who"))))

    }

    def withTimeBucketColum(timeBucketCol: String, timeAggregation: String): DataFrame = {
      dataFrame
        .withColumn(timeBucketCol, date_trunc(format=timeAggregation, from_unixtime(col("time_at_start").divide(1000))))
    }

    def withCommandColumns(commandCol: String, commandArgsCol: String): DataFrame = {
      dataFrame
        .withColumn(commandCol,
          extractCommandUDF(
            col("what"),
            col("access_path"),
            ifExistThenGetOrNull("http_method", col("http_method"))))
        .withColumn(commandArgsCol, extractCommandArgumentsUDF(col("what"), col("access_path")))
    }

    def aggregateNumEventsColumn(numEventsCol: String, cols: List[String]): DataFrame = {
      dataFrame.groupBy(cols.map(c => col(c)): _*)
        .agg(count("*")
        .alias(numEventsCol))
    }
  }
}
