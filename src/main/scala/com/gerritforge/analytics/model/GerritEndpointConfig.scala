package com.gerritforge.analytics.model

case class GerritEndpointConfig(baseUrl: String = "",
                                outputDir: String = s"${System.getProperty("java.io.tmp")}/analytics-${System.nanoTime()}",
                                elasticIndex: Option[String] = None,
                                since: Option[String] = None,
                                until: Option[String] = None,
                                aggregate: Option[String] = None) {

  def queryOpt(opt: (String, Option[String])): Option[String] = {
    opt match {
      case (name: String, value: Option[String]) => value.map(name + "=" + _)
    }
  }

  val queryString = Seq("since" -> since, "until" -> until, "aggregate" -> aggregate)
    .flatMap(queryOpt).mkString("?", "&", "")

  def contributorsUrl(projectName: String) =
    s"${baseUrl}/projects/$projectName/analytics~contributors${queryString}"
}
