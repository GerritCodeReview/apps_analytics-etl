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
  import ESSparkWriterImplicits.withAliasSwap
  import spark.implicits._

  "Saving and reading from same ES alias" must "work while changing indices mapping" in {

    val aliasName    = "the_alias"
    val documentName = "doc"

    // Writing into the first index
    val dataIntoIndexOne: Dataset[String] = "Content in the first index".split(" ").toList.toDS()
    dataIntoIndexOne.saveToEsWithAliasSwap(aliasName, documentName)
    // Reading from the alias
    val resultFromAliasFirst: Dataset[String] =
      spark.read.format("es").load(s"$aliasName/$documentName").as[String]

    // Written should equal Read
    dataIntoIndexOne
      .collect()
      .toList should contain only (resultFromAliasFirst.collect().toList: _*)

    // Writing into the second index
    val dataIntoIndexTwo: Dataset[String] = "Content in the second index".split(" ").toList.toDS()
    dataIntoIndexTwo.saveToEsWithAliasSwap(aliasName, documentName)
    // Reading from the alias
    val resultFromAliasSecond: Dataset[String] =
      spark.read.format("es").load(s"$aliasName/$documentName").as[String]

    // Written should equal Read
    dataIntoIndexTwo
      .collect()
      .toList should contain only (resultFromAliasSecond.collect().toList: _*)
  }

  "Saving data into ES" must "succeed even if alias creation fails" in {
    import org.elasticsearch.spark.sql._
    val indexWithAliasName     = "alias_name"
    val documentName           = "doc"
    val indexWithAliasNameData = List("This", "index", "will", "make", "alias", "creation", "fail")

    indexWithAliasNameData.toDS.saveToEs(s"$indexWithAliasName/$documentName")

    val indexWithAliasData = List("An", "index", "with", "alias")
    val (aliasCreationReponse, newIndexPath) =
      indexWithAliasData.toDS.saveToEsWithAliasSwap(indexWithAliasName, documentName)

    val oldIndexData =
      spark.read.format("es").load(s"$indexWithAliasName/$documentName").as[String]
    val newIndexData = spark.read.format("es").load(newIndexPath).as[String]

    aliasCreationReponse.isError shouldEqual (true)
    newIndexData.collect().toList should contain only (indexWithAliasData: _*)
    oldIndexData.collect().toList should contain only (indexWithAliasNameData: _*)
  }
}
