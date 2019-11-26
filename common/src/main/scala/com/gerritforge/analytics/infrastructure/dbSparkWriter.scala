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

import java.sql.{Connection, DriverManager}
import java.time.Instant
import java.util.Properties

import com.gerritforge.analytics.common.api.EnrichedAliasActionResponse
import com.gerritforge.analytics.common.api.db.RelationalDatabaseViewOps
import com.gerritforge.analytics.support.ops.IndexNameGenerator
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.Dataset

import scala.concurrent.Future

object DBSparkWriterImplicits {
  implicit def withDbWriter[T](data: Dataset[T]): RelationalDatabasePimpedWriter[T] =
    new RelationalDatabasePimpedWriter[T](data)

}

class RelationalDatabasePimpedWriter[T](dataset: Dataset[T]) extends RelationalDatabaseViewOps
  with LazyLogging{

  def saveToDb(jdbcString: String, tableName: String, additionalProperties: Properties =  new Properties()): EnrichedAliasActionResponse = {
    val newIndexNameWithTime = IndexNameGenerator.timeBasedIndexName(tableName, Instant.now())

    logger.info(
      s"Storing data into table $newIndexNameWithTime and updating view $tableName to read from the new table")

    // Save data
    val futureResponse: Future[Boolean] = try {
      dataset.write
        .jdbc(jdbcString, newIndexNameWithTime, additionalProperties)

      logger.info(
        s"Successfully stored the data into table $newIndexNameWithTime. Will now update the view $tableName")


      implicit val jdbcConnection: Connection = {
        if (additionalProperties.containsKey("driver")) {
         Class.forName(additionalProperties.getProperty("driver"))
        }
        DriverManager.getConnection(jdbcString)
      }

      updateViewToTheNewTable(newIndexNameWithTime, tableName)

    } catch {
      case exception: Exception => Future.failed(exception)
    }

    EnrichedAliasActionResponse(futureResponse, newIndexNameWithTime)
  }

}
