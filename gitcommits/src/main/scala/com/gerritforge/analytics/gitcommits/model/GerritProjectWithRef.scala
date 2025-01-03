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

import com.gerritforge.analytics.support.ops.GerritSourceOps._
import com.google.gerrit.extensions.api.GerritApi
import com.google.inject.Inject
import org.json4s.native.JsonMethods.parse

import scala.io.Source
import scala.util.Try
case class GerritProjectWithRef(id: String, name: String, refName: Option[String] = None)

class GerritProjectsSupport @Inject()(gerritApi: GerritApi) {

  def getProject(projectName: String): Try[GerritProjectWithRef] = {
    val projectApi = gerritApi.projects().name(projectName)
    Try {
      val project = projectApi.get()
      GerritProjectWithRef(project.id, project.name)
    }
  }
}

object GerritProjectsSupport {

  def parseJsonProjectListResponse(jsonSource: Source): Seq[GerritProjectWithRef] = {
    parse(jsonSource.dropGerritPrefix.mkString).values
      .asInstanceOf[Map[String, Map[String, String]]]
      .mapValues(projectAttributes => projectAttributes("id"))
      .toSeq
      .map {
        case (name, id) => GerritProjectWithRef(id, name)
      }
  }
}

case class ProjectContributionSource(name: String, refName: Option[String], contributorsUrl: Option[String])
