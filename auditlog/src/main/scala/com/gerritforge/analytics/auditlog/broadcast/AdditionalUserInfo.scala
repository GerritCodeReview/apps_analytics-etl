package com.gerritforge.analytics.auditlog.broadcast

import com.gerritforge.analytics.auditlog.model.AuditLogETLConfig
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}

import scala.util.Try

case class AdditionalUsersInfo(usersInfo: Map[Int,AdditionalUserInfo]) {
  def getUserType(who: Int): String = usersInfo.get(who).map(_.`type`).getOrElse(AdditionalUserInfo.DEFAULT_USER_TYPE)
}

case class AdditionalUserInfo(id: Int, `type`: String)

object AdditionalUserInfo {
  val DEFAULT_USER_TYPE = "human"
  var empty: AdditionalUsersInfo = AdditionalUsersInfo(Map.empty[Int,AdditionalUserInfo])

  def loadAdditionalUserInfo(config: AuditLogETLConfig)(implicit spark: SparkSession): Try[AdditionalUsersInfo] = {

    val schema = new StructType()
      .add("id", IntegerType,false)
      .add("type", StringType,false)

    import spark.implicits._
    Try {
      AdditionalUsersInfo(
        config.additionalUserInfoPath.map { path =>
            spark.read
              .option("header", "true")
              .schema(schema)
              .csv(path)
              .as[AdditionalUserInfo]
              // We are collecting on the fair assumption that the additional user info file will fit in memory
              .collect
              .map(additionalUserInfo => additionalUserInfo.id -> additionalUserInfo)
              .toMap
        }.getOrElse(Map.empty[Int,AdditionalUserInfo])
      )
    }
  }
}
