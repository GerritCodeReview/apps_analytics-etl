package com.gerritforge.analytics.auditlog.spark.rdd.ops
import com.gerritforge.analytics.auditlog.model.AuditEvent
import com.gerritforge.analytics.auditlog.range.TimeRange
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}

object SparkRDDOps {

  implicit class PimpedAuditEventRDD(rdd: RDD[AuditEvent]) {
    def filterWithinRange(timeRange: TimeRange): RDD[AuditEvent] =
      rdd.filter(event => timeRange.isWithin(event.timeAtStart))

    def toJsonString: RDD[String] = rdd.map(_.toJsonString)
  }

  implicit class PimpedStringRDD(rdd: RDD[String]) {
    def toJsonTableDataFrame(implicit spark: SparkSession): DataFrame = {
      import spark.implicits._
      spark.sqlContext.read.json(rdd.toDS())
    }
  }

}
