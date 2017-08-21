package com.gerritforge.analytics.job

import com.gerritforge.analytics.model.{GerritEndpointConfig, GerritProjects, ProjectContribution}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.io.{Codec, Source}
import org.elasticsearch.spark._

object Main extends App with Job {

  new scopt.OptionParser[GerritEndpointConfig]("scopt") {
    head("scopt", "3.x")
    opt[String]('u', "url") optional() action { (x, c) =>
      c.copy(baseUrl = x)
    } text "gerrit url"
    opt[String]('o', "out") optional() action { (x, c) =>
      c.copy(outputDir = x)
    } text "output directory"
    opt[String]('e', "elasticIndex") optional() action { (x, c) =>
      c.copy(elasticIndex = Some(x))
    } text "output directory"
    opt[String]('s', "since") optional() action { (x, c) =>
      c.copy(since = Some(x))
    } text "begin date "
    opt[String]('u', "until") optional() action { (x, c) =>
      c.copy(until = Some(x))
    } text "since date"
    opt[String]('g', "aggregate") optional() action { (x, c) =>
      c.copy(aggregate = Some(x))
    } text "aggregate email/email_hour/email_day/email_month/email_year"
  }.parse(args, GerritEndpointConfig()) match {
    case Some(config) =>
      val sparkConf = new SparkConf().setAppName("Gerrit Analytics ETL")
      val sc = new SparkContext(sparkConf)

      val outRDD = run(config, sc)
      outRDD.saveAsTextFile(config.outputDir)
      saveES(config,outRDD)


    case None => // invalid configuration usage has been displayed
  }
}

trait Job {
  implicit val codec = Codec.ISO8859

  import com.gerritforge.analytics.engine.GerritAnalyticsTrasformations._

  def run(implicit config: GerritEndpointConfig, sc: SparkContext): RDD[ProjectContribution] = {
    val rdd: RDD[String] = sc.parallelize(GerritProjects(Source.fromURL(s"${config.baseUrl}/projects/")))

    rdd.enrichWithSource(config).fetchContributors
  }
  def saveES(implicit config: GerritEndpointConfig, rdd: RDD[ProjectContribution]) = {
      config.elasticIndex.map(rdd.toJson().saveJsonToEs(_))
  }
}

