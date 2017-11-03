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

package com.gerritforge.analytics.engine

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}

import com.gerritforge.analytics.model.{GerritEndpointConfig, ProjectContribution, ProjectContributionSource}
import org.apache.spark.rdd.RDD
import org.json4s.JsonAST.{JField, JInt, JString}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.JavaConverters._

object GerritAnalyticsTrasformations {

  private[analytics] def longDateToISO(in: Number): String =
    ZonedDateTime.ofInstant(
      LocalDateTime.ofEpochSecond(in.longValue() / 1000L, 0, ZoneOffset.UTC),
      ZoneOffset.UTC, ZoneId.of("Z")
    ) format DateTimeFormatter.ISO_OFFSET_DATE_TIME

  private[analytics] val emailToDomain: PartialFunction[JValue,String] = {
    case JString(email) if email.contains("@") && !email.endsWith("@") =>
      email.split("@").last.toLowerCase
  }

  private[analytics] def transformLongDateToISO(in: String): JObject = {
    parse(in).transformField {
      case JField(fieldName, JInt(v)) if (fieldName=="date" || fieldName=="last_commit_date") =>
        JField(fieldName, JString(longDateToISO(v)))
    }.asInstanceOf[JObject]
  }

  private[analytics] def transformAddOrganization(in: JObject): JObject = {
    Some(in \ "email")
      .collect(emailToDomain)
      .fold(in)(org => ("organization" -> org) ~ in)
  }

  def getFileContentAsProjectContributions(sourceUrl: String, projectName: String): Iterator[ProjectContribution] = {
    val is = new URL(sourceUrl).openConnection.getInputStream
    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
      .lines.iterator().asScala
      .filterNot(_.trim.isEmpty)
      .map(transformLongDateToISO)
      .map(transformAddOrganization)
      .map(ProjectContribution(projectName, _))

  }

  implicit class PimpedRddOfProjectContributionSource(val rdd: RDD[ProjectContributionSource]) extends AnyVal {

    def fetchContributors: RDD[ProjectContribution] = {
      rdd.flatMap {
        case ProjectContributionSource(projectName, sourceUrl) =>
          try {
            getFileContentAsProjectContributions(sourceUrl, projectName)
          } catch {
            case ioex: IOException => None
          }
      }
    }
  }

  implicit class PimpedRddOfProjects(val rdd: RDD[String]) extends AnyVal {

    def enrichWithSource(config: GerritEndpointConfig) = {
      rdd.map { projectName =>
        ProjectContributionSource(projectName, config.contributorsUrl(projectName))
      }
    }
  }

  implicit class PimpedRddOfProjects2Json(val rdd: RDD[ProjectContribution]) extends AnyVal {
    def toJson() = {
      rdd.map(pc =>
        compact(render(
          ("project" -> pc.projectName) ~ pc.authorContribution)
        )
      )
    }
  }

}
