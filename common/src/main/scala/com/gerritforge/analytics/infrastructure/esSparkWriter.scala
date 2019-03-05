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
import scala.util.Try

object ESSparkWriterImplicits {
  implicit def withAliasSwap[T](data: Dataset[T]): ElasticSearchPimpedWriter[T] =
    new ElasticSearchPimpedWriter[T](data)
}

class ElasticSearchPimpedWriter[T](data: Dataset[T])
    extends ElasticSearchAliasOps
    with LazyLogging
    with SparkEsClientProvider {

  def saveToEsWithAliasSwap(aliasName: String,
                            documentType: String): (Future[Option[AliasActionResponse]], String) = {
    val newIndexNameWithTime = IndexNameGenerator.timeBasedIndexName(aliasName, Instant.now())
    val newPersistencePath   = s"$newIndexNameWithTime/$documentType"

    logger.info(
      s"Storing data into $newPersistencePath and swapping alias $aliasName to read from the new index")

    import scala.concurrent.ExecutionContext.Implicits.global
    // Save data
    val result = Try(
      data
        .toDF()
        .saveToEs(newPersistencePath)
    ).map { _ =>
        logger.info(
          s"Successfully stored the data into index $newIndexNameWithTime. Will now update the alias $aliasName")

        val idxSwapResult: Future[Option[AliasActionResponse]] =
          moveAliasToNewIndex(aliasName, newIndexNameWithTime).map { response =>
            response.isSuccess match {
              case true =>
                response.result.success match {
                  case true =>
                    logger.info("Alias was updated successfully")
                    Some(response.result)
                  case false =>
                    logger.error(
                      s"Alias update failed with response result error ${response.result}")
                    None
                }
              case false =>
                logger.error(s"Alias update failed with response error ${response.error.`type`}")
                None
            }
          }
        idxSwapResult
      }
      .getOrElse(Future(None))

    (result, newPersistencePath)
  }

  override val esSparkSession: SparkSession = data.sparkSession
}
