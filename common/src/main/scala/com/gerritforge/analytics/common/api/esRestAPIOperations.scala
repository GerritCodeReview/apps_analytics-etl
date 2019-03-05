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

import org.apache.spark.sql.SparkSession
import org.elasticsearch.hadoop.cfg.PropertiesSettings
import org.elasticsearch.spark.cfg.SparkSettingsManager

import scala.io.{BufferedSource, Codec, Source}
import scala.util.Try

trait ESRestAPISparkClient {

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
    println(data)
    writer.write(data.getBytes)
    writer.flush()
    writer.close()

    Source.fromInputStream(connection.getInputStream)
  }

  def readFromES(relativePath: String): BufferedSource = {
    val fullUrlPath = s"$esRestBaseUrl/$relativePath"
    println(fullUrlPath)

    Source.fromURL(fullUrlPath, Codec.UTF8.name)
  }
}

trait ESRestApiExtraOps extends ESRestAPISparkClient {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._

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

  def replaceAliasOldIndicesWithNew(aliasName: String, newIndexName: String) = {
    val oldIndicesRemoveAction = getIndicesFromAlias(aliasName)
      .map { oldIndexName =>
        s"""{ "remove" : { "index" : "$oldIndexName", "alias" : "$aliasName" } },"""
      }
    val newAliasAddAction = s"""{ "add" : { "index" : "$newIndexName", "alias" : "$aliasName" } }"""

    val postAction = s"""{
                        |"actions" : [
                        |  ${oldIndicesRemoveAction.mkString}
                        |  $newAliasAddAction
                        |]
                        |}""".stripMargin

    postToES("_aliases", postAction)
  }
}
