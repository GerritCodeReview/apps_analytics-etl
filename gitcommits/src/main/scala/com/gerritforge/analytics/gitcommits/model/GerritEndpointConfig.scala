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

import java.net.URLEncoder
import scala.xml._
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.support.ops.AnalyticsDateTimeFormatter
case class GerritEndpointConfig(
    baseUrl: Option[String] = None,
    prefix: Option[String] = None,
    outputDir: String =
      s"file://${System.getProperty("java.io.tmpdir")}/analytics-${System.nanoTime()}",
    elasticIndex: Option[String] = None,
    since: Option[LocalDate] = None,
    until: Option[LocalDate] = None,
    aggregate: Option[String] = None,
    emailAlias: Option[String] = None,
    username: Option[String] = None,
    password: Option[String] = None,
    ignoreSSLCert: Option[Boolean] = None,
    extractBranches: Option[Boolean] = None,
    manifest: Option[String] = None
) {

  lazy val manifestProjectNames: Option[Set[String]] = manifest.map { mf =>
      val mfDoc = XML.loadFile(mf)
      val mfProjects = mfDoc \ "project"
    mfProjects.theSeq
      .flatMap(_.attribute("name").toSeq)
      .flatten
      .map(_.text)
      .toSet
  }

  val projectInManifest: GerritProject => Boolean = (project: GerritProject) => manifestProjectNames.fold(true)(_.contains(project.name))

  val gerritApiConnection: GerritConnectivity =
    new GerritConnectivity(username, password, ignoreSSLCert.getOrElse(false))

  val gerritProjectsUrl: Option[String] = baseUrl.map { url =>
    s"${url}/projects/" + prefix.fold("")("?p=" + _)
  }

  def queryOpt(opt: (String, Option[String])): Option[String] = {
    opt match {
      case (name: String, value: Option[String]) => value.map(name + "=" + _)
    }
  }

  @transient
  private lazy val format: DateTimeFormatter =
    AnalyticsDateTimeFormatter.yyyy_MM_dd.withZone(ZoneOffset.UTC)
  val queryString = Seq(
    "since"            -> since.map(format.format),
    "until"            -> until.map(format.format),
    "aggregate"        -> aggregate,
    "extract-branches" -> extractBranches.map(_.toString)
  ).flatMap(queryOpt).mkString("?", "&", "")

  def contributorsUrl(projectName: String): Option[String] =
    baseUrl.map { url =>
      s"$url/projects/$projectName/analytics~contributors$queryString"
    }
}
