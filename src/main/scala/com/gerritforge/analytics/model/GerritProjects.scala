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

import org.json4s.JObject
import org.json4s.native.JsonMethods.parse

import scala.io.Source

object GerritProjects {

  type GerritProjects = Seq[String]

  val GERRIT_PREFIX_LEN = ")]}'\n".length

  def apply(jsonSource: Source) =
    parse(jsonSource.drop(GERRIT_PREFIX_LEN).mkString)
      .asInstanceOf[JObject]
      .values
      .keys
      .toSeq
}

case class ProjectContributionSource(name: String, contributorsUrl: String)

