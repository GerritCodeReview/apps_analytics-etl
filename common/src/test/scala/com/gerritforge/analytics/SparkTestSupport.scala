// Copyright (C) 2018 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.analytics

import org.apache.spark.SparkContext
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait SparkTestSupport extends BeforeAndAfterAll { this: Suite =>

  implicit val spark : SparkSession = SparkSession.builder()
    .config("es.nodes.wan.only", "true")
    .master("local[4]")
    .getOrCreate()

  implicit lazy val sc: SparkContext = spark.sparkContext
  implicit lazy val sql: SQLContext = spark.sqlContext

  override protected def afterAll() = {
    spark.close()
  }
}
