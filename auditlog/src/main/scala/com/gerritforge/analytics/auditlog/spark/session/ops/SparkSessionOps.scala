package com.gerritforge.analytics.auditlog.spark.session.ops

import com.gerritforge.analytics.auditlog.model.AuditEvent
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object SparkSessionOps {

  implicit class PimpedSparkSession(spark: SparkSession) {
    def getEventsFromPath(path: String): RDD[AuditEvent] =
      spark
        .read
        .textFile(path)
        .rdd
        .flatMap(auditString => AuditEvent.parseRaw(auditString).toOption)
  }
}
