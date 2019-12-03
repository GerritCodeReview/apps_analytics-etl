// Copyright (C) 2017 GerritForge Ltd
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

package com.gerritforge.analytics.gitcommits.model
import java.time.{LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter

import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.support.ops.AnalyticsDateTimeFormatter
import com.typesafe.config.Config

object GerritEndpointConfig {
  val BASE_URL         = "common.base-url"
  val PREFIX           = "common.prefix"
  val OUTPUT_DIR       = "common.output-dir"
  val SINCE            = "common.since"
  val UNTIL            = "common.until"
  val AGGREGATE        = "common.aggregate"
  val EMAIL_ALIAS      = "common.email-alias"
  val USERNAME         = "common.username"
  val PASSWORD         = "common.password"
  val IGNORE_SSL_CERT  = "common.ignore-ssl-cert"
  val EXTRACT_BRANCHES = "common.extract-branches"

  val SAVE_MODE = "saveMode"

  val ELASTIC_SEARCH_INDEX = "elastic-search.index"

  val RELATIONAL_DATABASE_DRIVER          = "relational-database.driver"
  val RELATIONAL_DATABASE_JDBC_CONNECTION = "relational-database.jdbc-connection"
  val RELATIONAL_DATABASE_TABLE           = "relational-database.table"

  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def getOptional[T](path: String): Option[T] =
      if (underlying.hasPath(path)) {
        Some(underlying.getAnyRef(path).asInstanceOf[T])
      } else {
        None
      }

    def contributorsUrl(projectName: String): Option[String] =
      getOptional[String](BASE_URL).map { url =>
        s"$url/projects/$projectName/analytics~contributors$queryString"
      }

    def gerritProjectsUrl: Option[String] = getOptional[String](BASE_URL).map { url =>
      s"${url}/projects/" + getOptional[String](PREFIX).fold("")("?p=" + _)
    }

    def gerritApiConnection: GerritConnectivity =
      new GerritConnectivity(getOptional[String](USERNAME),
                             getOptional[String](PASSWORD),
                             getOptional[Boolean](IGNORE_SSL_CERT).getOrElse(false))

    def queryOpt(opt: (String, Option[String])): Option[String] = {
      opt match {
        case (name: String, value: Option[String]) => value.map(name + "=" + _)
      }
    }

    def outputDir =
      getOptional[String](OUTPUT_DIR).getOrElse(
        s"file://${System.getProperty("java.io.tmpdir")}/analytics-${System.nanoTime()}")

    @transient
    private def format: DateTimeFormatter =
      AnalyticsDateTimeFormatter.yyyy_MM_dd.withZone(ZoneOffset.UTC)
    def queryString =
      Seq(
        "since"            -> getOptional[Long](SINCE).map(s => format.format(LocalDate.ofEpochDay(s))),
        "until"            -> getOptional[Long](UNTIL).map(s => format.format(LocalDate.ofEpochDay(s))),
        "aggregate"        -> getOptional[String](AGGREGATE),
        "extract-branches" -> getOptional[Boolean](EXTRACT_BRANCHES).map(_.toString)
      ).flatMap(queryOpt).mkString("?", "&", "")

  }
}
