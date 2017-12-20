package com.gerritforge.analytics.model

import org.scalatest.{FlatSpec, Matchers}

class GerritEndpointConfigTest extends FlatSpec with Matchers {

  "gerritProjectsUrl" should "contain prefix when available" in {
    val prefix = "prefixMustBeThere"
    val conf = GerritEndpointConfig(baseUrl = "testBaseUrl", prefix = Some(prefix))
    conf.gerritProjectsUrl shouldBe s"testBaseUrl/projects/?p=$prefix"
  }

  it should "not contain prefix when not available" in {
    val conf = GerritEndpointConfig(baseUrl = "testBaseUrl", prefix = None)
    conf.gerritProjectsUrl shouldBe s"testBaseUrl/projects/"
  }

}
