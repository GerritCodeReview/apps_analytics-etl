package com.gerritforge.analytics.model

import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class GerritProjectsTest extends FlatSpec with Matchers {

  "GerritProjects" should "use the 'id' as identifier" in {
    val projectId = "apps%2Freviewit" // URL_Encode(project_key) => project_id (i.e.: app/reviewit => apps%2Freviewit)
    val source = Source.fromString(GerritProjects.GERRIT_PREFIX + s"""{"app/reviewit": {"id":"$projectId"}}""")
    GerritProjects(source) shouldBe Seq(projectId)
  }
}