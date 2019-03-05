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
import java.io.DataOutputStream
import java.net.{HttpURLConnection, URL}

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.SparkSession
import org.elasticsearch.hadoop.cfg.PropertiesSettings
import org.elasticsearch.spark.cfg.SparkSettingsManager

import scala.io.{BufferedSource, Codec, Source}

trait ESRestAPISparkClient {

  private val logger = Logger(classOf[ESRestAPISparkClient])

  val spark: SparkSession

  private lazy val sparkCfg =
    new SparkSettingsManager()
      .load(spark.sqlContext.sparkContext.getConf)
  lazy val esCfg = new PropertiesSettings()
    .load(sparkCfg.save())

  lazy val esRestBaseUrl = s"http://${esCfg.getNodes}:${esCfg.getPort}"

  def postToES(relativePath: String, data: String): BufferedSource = {
    val fullPath = s"$esRestBaseUrl/$relativePath"

    val connection = new URL(fullPath).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setConnectTimeout(5000)
    connection.setRequestProperty("Content-Type", "application/json")
    val writer = new DataOutputStream(connection.getOutputStream())
    logger.info(s"Data posted to ES: $data")
    writer.write(data.getBytes)
    writer.flush()
    writer.close()

    Source.fromInputStream(connection.getInputStream)
  }

  def readFromES(relativePath: String): BufferedSource = {
    val fullUrlPath = s"$esRestBaseUrl/$relativePath"
    logger.info(fullUrlPath)
    Source.fromURL(fullUrlPath, Codec.UTF8.name)
  }
}

trait ESRestApiExtraOps extends ESRestAPISparkClient {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._

  private val logger = Logger(classOf[ESRestApiExtraOps])

  def getIndicesFromAlias(aliasName: String): List[String] = {

    val result: String = try {
      readFromES(aliasName).mkString
    } catch {
      case e: java.io.FileNotFoundException => "{}"
      case e: Exception                     => throw e
    }

    parse(result) match {
      case JObject(indices) => indices.map(_._1)
      case _                => List.empty[String]
    }
  }

  def replaceAliasOldIndicesWithNew(aliasName: String, newIndexName: String): Unit = {
    val oldIndices = getIndicesFromAlias(aliasName)
    val oldIndicesRemoveAction = oldIndices.map { oldIndexName =>
      s"""{ "remove" : { "index" : "$oldIndexName", "alias" : "$aliasName" } },"""
    }
    val newIndexAddAction = s"""{ "add" : { "index" : "$newIndexName", "alias" : "$aliasName" } }"""

    val postAction = s"""{
                        |"actions" : [
                        |  ${oldIndicesRemoveAction.mkString}
                        |  $newIndexAddAction
                        |]
                        |}""".stripMargin

    logger.info(
      s"Replacing alias old indices (${oldIndices.mkString(",")}) with new index $newIndexName")
    val postResult = postToES("_aliases", postAction)
    logger.info(s"Result for rebuilding the alias: ${postResult.mkString}")
  }
}
