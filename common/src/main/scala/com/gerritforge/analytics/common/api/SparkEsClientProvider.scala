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

package com.gerritforge.analytics.common.api
import com.sksamuel.elastic4s.http.ElasticClient
import org.apache.http.HttpHost
import org.apache.spark.sql.SparkSession
import org.elasticsearch.client.RestClient
import org.elasticsearch.hadoop.cfg.{PropertiesSettings, Settings}
import org.elasticsearch.spark.cfg.SparkSettingsManager

trait SparkEsClientProvider {

  val esSparkSession: SparkSession

  private lazy val sparkCfg =
    new SparkSettingsManager()
      .load(esSparkSession.sqlContext.sparkContext.getConf)

  private lazy val esCfg: Settings = new PropertiesSettings()
    .load(sparkCfg.save())

  private lazy val restClient: RestClient =
    RestClient.builder(new HttpHost(esCfg.getNodes, esCfg.getPort, "http")).build()

  lazy val esClient: ElasticClient = ElasticClient.fromRestClient(restClient)
}