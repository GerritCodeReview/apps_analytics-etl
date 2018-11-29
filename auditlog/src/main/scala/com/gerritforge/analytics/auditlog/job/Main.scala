package com.gerritforge.analytics.auditlog.job
import com.gerritforge.analytics.auditlog.model.AuditEvent
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark

object Main extends App with LazyLogging {

  val eventsPath: String = "/Users/syntonyze/dev/gerrit/audit_log"
  val elasticSearchHost = "127.0.0.1"

  implicit val spark: SparkSession = SparkSession
    .builder()
    .appName("Gerrit AuditLog Analytics ETL")
    .config("spark.master", "local")
    .config("spark.es.nodes", elasticSearchHost)
    .config("spark.es.port", 9200)
    .config("es.index.auto.create", "true")
    .getOrCreate()

  val auditJsonStringRDD: RDD[String] = spark
    .read
    .textFile(eventsPath)
    .rdd
    .flatMap(auditString => AuditEvent.fromJsonString(auditString).toOption)
    .map(AuditEvent.toJsonString)

  JavaEsSpark.saveJsonToEs(auditJsonStringRDD, resource="gerrit/auditlog")
}
