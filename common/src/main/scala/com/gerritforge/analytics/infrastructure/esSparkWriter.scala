// Copyright (C) 2019 GerritForge Ltd
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

package com.gerritforge.analytics.infrastructure
import java.time.{Instant, LocalDateTime, ZoneId}

import com.gerritforge.analytics.common.api.{ElasticSearchAliasOps, SparkEsClientProvider}
import com.gerritforge.analytics.support.ops.AnalyticsDateTimeFormatter
import com.sksamuel.elastic4s.http.Response
import com.sksamuel.elastic4s.http.index.admin.AliasActionResponse
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{Dataset, SparkSession}
import org.elasticsearch.spark.sql._

import scala.concurrent.Future

object ESSparkWriterImplicits {
  implicit def withAliasSwap[T](data: Dataset[T]): ElasticSearchPimpedWriter[T] =
    new ElasticSearchPimpedWriter[T](data)
}

class ElasticSearchPimpedWriter[T](data: Dataset[T])
    extends ElasticSearchAliasOps
    with LazyLogging
    with SparkEsClientProvider {

  def saveToEsWithAliasSwap(aliasName: String,
                            documentType: String): Future[Response[AliasActionResponse]] = {
    val now: Long = Instant.now().toEpochMilli
    val dateWithStrFormat: String =
      LocalDateTime
        .ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
        .format(AnalyticsDateTimeFormatter.yyyy_MM_dd)

    val newIndexNameWithTime = s"${aliasName}_${dateWithStrFormat}_$now"
    val newPersistencePath   = s"$newIndexNameWithTime/$documentType"

    logger.info(
      s"Storing data into $newPersistencePath and swapping alias $aliasName to read from the new index")

    // Save data
    data
      .toDF()
      .saveToEs(newPersistencePath)

    // Replace alias
    replaceAliasOldIndicesWithNew(aliasName, newIndexNameWithTime)
  }
  override val esSparkSession: SparkSession = data.sparkSession
}
