package com.gerritforge.analytics.auditlog.model
import org.json4s.jackson.Serialization.write
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.util.{Success, Try}


case class AuditEvent(
  sessionId: String,
  who: Option[Int],
  accessPath: Option[String],
  timeAtStart: Long,
  httpMethod: Option[String],
  what: String,
  elapsed: Int,
  uuid: String,
  auditType: String
) {
  implicit private val formats = DefaultFormats

  def toJsonString: String = {
    compact(render(parse(write[AuditEvent](this)).snakizeKeys))
  }

}

object AuditEvent {
  implicit private val formats = DefaultFormats

  def parseRaw(json: String): Try[AuditEvent] =  Try(parse(json)).flatMap { jsValueEvent =>

    val auditType = (jsValueEvent \ "type").extract[String]
    val who = (jsValueEvent \ "event" \ "who" \ "account_id" \ "id").extractOpt[Int]
    val accessPath = (jsValueEvent \ "event" \ "who" \ "access_path").extractOpt[String]
    val timeAtStart = (jsValueEvent \ "event" \ "time_at_start").extract[Long]
    val httpMethod = (jsValueEvent \ "event" \ "http_method").extractOpt[String]
    val what = (jsValueEvent \ "event" \ "what").extract[String]
    val elapsed = (jsValueEvent \ "event" \ "elapsed").extract[Int]
    val uuid = (jsValueEvent \ "event" \ "uuid" \ "uuid").extract[String]
    val sessionId = (jsValueEvent \ "event" \ "session_id").extract[String]

    Success(
      AuditEvent(sessionId, who, accessPath, timeAtStart, httpMethod, what, elapsed, uuid, auditType)
    )
  }
}