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
import com.gerritforge.analytics.SparkTestSupport
import com.gerritforge.analytics.support.ops.ElasticsearchTestITSupport
import org.apache.spark.sql.Dataset
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ElasticSearchPimpedWriterIT
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with SparkTestSupport
    with ElasticsearchTestITSupport {

  override lazy val elasticSearchConnPort: Int = esHostHTTPExtPortMapping

  "Saving and reading from same ES alias" must "work while changing indices mapping" in {

    import ESSparkWriterImplicits.withAliasSwap
    import spark.implicits._
    val aliasName = "the_alias"
    val document  = "doc"

    // Writing into the first index
    val dataIntoIndexOne: Dataset[String] = "Content in the first index".split(" ").toList.toDS()
    dataIntoIndexOne.saveToEsWithAliasSwap(aliasName, document)
    // Reading from the alias
    val resultFromAliasFirst: Dataset[String] =
      spark.read.format("es").load(s"$aliasName/$document").as[String]

    // Written should equal Read
    dataIntoIndexOne
      .collect()
      .toList should contain only (resultFromAliasFirst.collect().toList: _*)

    // Writing into the second index
    val dataIntoIndexTwo: Dataset[String] = "Content in the second index".split(" ").toList.toDS()
    dataIntoIndexTwo.saveToEsWithAliasSwap(aliasName, document)
    // Reading from the alias
    val resultFromAliasSecond: Dataset[String] =
      spark.read.format("es").load(s"$aliasName/$document").as[String]

    // Written should equal Read
    dataIntoIndexTwo
      .collect()
      .toList should contain only (resultFromAliasSecond.collect().toList: _*)
  }
}
