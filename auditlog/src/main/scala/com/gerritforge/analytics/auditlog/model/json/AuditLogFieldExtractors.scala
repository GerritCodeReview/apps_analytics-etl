package com.gerritforge.analytics.auditlog.model.json
import org.json4s.{DefaultFormats, JValue}

trait AuditLogFieldExtractors {
  implicit private val formats = DefaultFormats

  val accessPath  = (jsEvent: JValue) => (jsEvent \ "who" \ "access_path").extractOpt[String]
  val sessionId   = (jsEvent: JValue) => (jsEvent \ "session_id").extract[String]
  val timeAtStart = (jsEvent: JValue) => (jsEvent \ "time_at_start").extract[Long]
  val what        = (jsEvent: JValue) => (jsEvent \ "what").extract[String]
  val elapsed     = (jsEvent: JValue) => (jsEvent \ "elapsed").extract[Int]
  val uuid        = (jsEvent: JValue) => (jsEvent \ "uuid" \ "uuid").extract[String]
  val who         = (jsEvent: JValue) => (jsEvent \ "who" \ "account_id" \ "id").extractOpt[Int]
  val httpMethod  = (jsEvent: JValue) => (jsEvent \ "http_method").extract[String]
  val httpStatus  = (jsEvent: JValue) => (jsEvent \ "http_status").extract[Int]

}
