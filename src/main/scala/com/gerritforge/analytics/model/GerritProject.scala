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

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.json4s.native.JsonMethods.parse

import scala.io.Source

case class GerritProject(id: String, name: String)

object GerritProjectsRDD {

  val GERRIT_PREFIX = ")]}'\n"
  private val GERRIT_PREFIX_LEN = GERRIT_PREFIX.length

  def apply(jsonSource: Source)(implicit sc: SparkContext): RDD[GerritProject] =
    sc.parallelize(
      parse(jsonSource.drop(GERRIT_PREFIX_LEN).mkString)
        .values
        .asInstanceOf[Map[String, Map[String, String]]]
        .mapValues(_ ("id"))
        .toSeq)
      .map {
        case (id, name) => GerritProject(id, name)
      }
}

case class ProjectContributionSource(name: String, contributorsUrl: String)