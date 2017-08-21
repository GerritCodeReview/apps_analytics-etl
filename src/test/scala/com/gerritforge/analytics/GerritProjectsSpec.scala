package com.gerritforge.analytics

import com.gerritforge.analytics.model.GerritProjects
import org.scalatest.{FlatSpec, Matchers}

import scala.io.{Codec, Source}

class GerritProjectsSpec extends FlatSpec with Matchers {

  "GerritProjects" should "parseProjects" in {
    val projectNames = GerritProjects(Source.fromString(
      """)]}'
        |{
        | "All-Projects": {
        |   "id": "All-Projects",
        |   "state": "ACTIVE"
        | },
        | "Test": {
        |   "id": "Test",
        |   "state": "ACTIVE"
        | }
        |}
        |""".stripMargin))

    projectNames should have size 2
    projectNames should contain allOf("All-Projects", "Test")
  }

}
