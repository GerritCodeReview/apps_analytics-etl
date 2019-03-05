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