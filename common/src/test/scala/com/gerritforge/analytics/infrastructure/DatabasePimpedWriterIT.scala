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
import java.util.Properties

import com.gerritforge.analytics.SparkTestSupport
import com.gerritforge.analytics.support.ops.DatabaseTestITSupport
import org.apache.spark.sql.Dataset
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class DatabasePimpedWriterIT extends FlatSpec
  with Matchers
  with BeforeAndAfterAll
  with SparkTestSupport
  with DatabaseTestITSupport {

  import DBSparkWriterImplicits.withDbWriter
  import spark.implicits._

  lazy val jdbcStr = s"jdbc:postgresql://localhost:$databaseHTTPIntPortMapping/$dbName?user=$user&password=$password"

  "Saving and reading from then same DB view" must "work while updating view mapping" in {
    val viewName = "the_view"

    // Writing into the first table
    val dataIntoTableOne: Dataset[String] = "Content in the first table".split(" ").toList.toDS()
    Await.result(dataIntoTableOne.saveToDb(jdbcStr, viewName).futureAction,
      2 seconds)
    // Reading from the alias
    val resultFromFirstView: Dataset[String] =
      spark.read
        .jdbc(jdbcStr, viewName, new Properties()).as[String]

    // Written should equal Read
    dataIntoTableOne
      .collect()
      .toList should contain only (resultFromFirstView.collect().toList: _*)

    // Writing into the second table
    val dataIntoTableTwo: Dataset[String] = "Content in the second table".split(" ").toList.toDS()
    Await.result(dataIntoTableTwo.saveToDb(jdbcStr, viewName).futureAction,
      2 seconds)
    // Reading from the alias
    val resultFromSecondView: Dataset[String] =
      spark.read
        .jdbc(jdbcStr, viewName, new Properties()).as[String]

    // Written should equal Read
    dataIntoTableTwo
      .collect()
      .toList should contain only (resultFromSecondView.collect().toList: _*)
  }

  "Saving data into database" must "succeed even if schema is changed" in {
    val viewName = "the_view"
    val firstTableData = List("This", "is", "the", "first", "table")

    Await.result(firstTableData.toDS.saveToDb(jdbcStr, viewName).futureAction, 2 seconds)
    val viedData =
      spark.read
        .jdbc(jdbcStr, viewName, new Properties()).as[String]

    viedData.collect().toList should contain only (firstTableData: _*)

    val secondTableData = List("Second", "table")
    Await.result(secondTableData.toDS.saveToDb(jdbcStr, viewName).futureAction, 2 seconds)
    val updatedViewData =
      spark.read
        .jdbc(jdbcStr,viewName, new Properties()).as[String]

    updatedViewData.collect().toList should contain only (secondTableData: _*)
  }
}
