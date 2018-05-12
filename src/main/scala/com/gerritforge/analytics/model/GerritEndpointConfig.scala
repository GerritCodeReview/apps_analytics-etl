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

package com.gerritforge.analytics.model

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import com.gerritforge.analytics.support.ops.AnalyticsTimeOps.AnalyticsDateTimeFormater

case class GerritEndpointConfig(baseUrl: String = "",
                                prefix: Option[String] = None,
                                outputDir: String = s"file://${System.getProperty("java.io.tmpdir")}/analytics-${System.nanoTime()}",
                                elasticIndex: Option[String] = None,
                                since: Option[LocalDate] = None,
                                until: Option[LocalDate] = None,
                                aggregate: Option[String] = None,
                                emailAlias: Option[String] = None,
                                eventsPath: Option[String] = None,
                                eventsFailureOutputPath: Option[String] = None
                               ) {

  val gerritProjectsUrl: String = s"${baseUrl}/projects/" + prefix.fold("")("?p=" + _)

  def queryOpt(opt: (String, Option[String])): Option[String] = {
    opt match {
      case (name: String, value: Option[String]) => value.map(name + "=" + _)
    }
  }

  @transient
  private lazy val format: DateTimeFormatter = AnalyticsDateTimeFormater.yyyy_MM_dd.withZone(ZoneOffset.UTC)
  val queryString = Seq("since" -> since.map(format.format), "until" -> until.map(format.format), "aggregate" -> aggregate)
    .flatMap(queryOpt).mkString("?", "&", "")

  def contributorsUrl(projectName: String) =
    s"$baseUrl/projects/$projectName/analytics~contributors$queryString"
}