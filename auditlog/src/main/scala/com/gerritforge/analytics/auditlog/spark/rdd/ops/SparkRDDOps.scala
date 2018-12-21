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

package com.gerritforge.analytics.auditlog.spark.rdd.ops

import com.gerritforge.analytics.auditlog.broadcast.{
  AdditionalUsersInfo,
  GerritProjects,
  GerritUserIdentifiers
}
import com.gerritforge.analytics.auditlog.model.{AggregatedAuditEvent, AuditEvent}
import com.gerritforge.analytics.auditlog.range.TimeRange
import com.gerritforge.analytics.auditlog.spark.AuditLogsTransformer
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

object SparkRDDOps {

  implicit class PimpedAuditEventRDD(rdd: RDD[AuditEvent]) {
    def filterWithinRange(timeRange: TimeRange): RDD[AuditEvent] =
      rdd.filter(event => timeRange.isWithin(event.timeAtStart))

    def toJsonString: RDD[String] = rdd.map(_.toJsonString)

    def transformEvents(
        gerritUserIdentifiers: GerritUserIdentifiers,
        additionalUsersInfo: AdditionalUsersInfo,
        gerritProjects: GerritProjects,
        timeAggregation: String,
        timeRange: TimeRange
    )(implicit spark: SparkSession): Dataset[AggregatedAuditEvent] = {

      AuditLogsTransformer(gerritUserIdentifiers, additionalUsersInfo, gerritProjects)
        .transform(rdd, timeAggregation, timeRange)
    }
  }

  implicit class PimpedStringRDD(rdd: RDD[String]) {
    def toJsonTableDataFrame(implicit spark: SparkSession): DataFrame = {
      import spark.implicits._
      spark.sqlContext.read.json(rdd.toDS())
    }
  }
}
