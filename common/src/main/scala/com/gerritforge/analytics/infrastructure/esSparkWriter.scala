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
import java.time.Instant

import com.gerritforge.analytics.common.api.{ElasticSearchAliasOps, SparkEsClientProvider}
import com.gerritforge.analytics.support.ops.IndexNameGenerator
import com.sksamuel.elastic4s.http.index.admin.AliasActionResponse
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{Dataset, SparkSession}
import org.elasticsearch.spark.sql._

import scala.concurrent.Future

case class EnrichedAliasActionResponse(futureAction: Future[AliasActionResponse], path: String)

object ESSparkWriterImplicits {
  implicit def withAliasSwap[T](data: Dataset[T]): ElasticSearchPimpedWriter[T] =
    new ElasticSearchPimpedWriter[T](data)
}

class ElasticSearchPimpedWriter[T](data: Dataset[T])
    extends ElasticSearchAliasOps
    with LazyLogging
    with SparkEsClientProvider {

  def saveToEsWithAliasSwap(
      aliasName: String,
      documentType: String
  ): EnrichedAliasActionResponse = {
    val newIndexNameWithTime = IndexNameGenerator.timeBasedIndexName(aliasName, Instant.now())
    val newPersistencePath   = s"$newIndexNameWithTime/$documentType"

    logger.info(
      s"Storing data into $newPersistencePath and swapping alias $aliasName to read from the new index"
    )

    import scala.concurrent.ExecutionContext.Implicits.global
    // Save data
    val futureResponse: Future[AliasActionResponse] = try {
      data
        .toDF()
        .saveToEs(newPersistencePath)

      logger.info(
        s"Successfully stored the data into index $newIndexNameWithTime. Will now update the alias $aliasName"
      )
      moveAliasToNewIndex(aliasName, newIndexNameWithTime).flatMap { response =>
        if (response.isSuccess && response.result.success) {
          logger.info("Alias was updated successfully")
          closeElasticsearchClientConn()
          Future.successful(response.result)
        } else {
          closeElasticsearchClientConn()
          logger.error(s"Alias update failed with response result error ${response.error}")
          logger.error(s"Alias update failed with ES ACK: ${response.result.acknowledged}")
          Future.failed(new Exception(s"Index alias $aliasName update failure ${response.error}"))
        }
      }
    } catch {
      case e: Exception =>
        Future.failed[AliasActionResponse](e)
    }
    EnrichedAliasActionResponse(futureResponse, newPersistencePath)
  }

  override val esSparkSession: SparkSession = data.sparkSession
}
