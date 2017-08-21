package com.gerritforge.analytics.model

import org.json4s.JObject
import org.json4s.native.JsonMethods.parse

import scala.io.Source

/**
  * Created by lucamilanesio on 22/08/2017.
  */
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

case class ProjectContribution(projectName: String, authorContribution: JObject)

