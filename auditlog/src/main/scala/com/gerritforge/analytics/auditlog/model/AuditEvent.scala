package com.gerritforge.analytics.auditlog.model
import com.gerritforge.analytics.auditlog.model.json.AuditLogFieldExtractors
import org.json4s.jackson.Serialization.write
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}
import org.json4s.JsonDSL._

import scala.util.{Failure, Try}


sealed trait AuditEvent {
  def auditType: String
  def accessPath: Option[String]
  def sessionId: String
  def timeAtStart: Long
  def what: String
  def elapsed: Int
  def uuid: String
  def who: Option[Int]

  implicit def formats: Formats = DefaultFormats

  def toJsonString: String =
    compact(
      render(
        parse(write[AuditEvent](this)).snakizeKeys merge JObject(JField("audit_type", auditType))
      )
    )
}

final case class SshAuditEvent(
  accessPath: Option[String],
  sessionId: String,
  who: Option[Int],
  timeAtStart: Long,
  what: String,
  elapsed: Int,
  uuid: String
) extends AuditEvent {

  override val auditType = SshAuditEvent.auditType

}

object SshAuditEvent extends AuditLogFieldExtractors {

  val auditType = "SSH"

  def apply(jsEvent: JValue): Try[SshAuditEvent] = Try {
    SshAuditEvent(
      accessPath(jsEvent),
      sessionId(jsEvent),
      who(jsEvent),
      timeAtStart(jsEvent),
      what(jsEvent),
      elapsed(jsEvent),
      uuid(jsEvent)
    )
  }
}

sealed trait BaseHttpAuditEvent extends AuditEvent {
  def httpMethod: String
  def httpStatus: Int
}

final case class HttpAuditEvent(
  accessPath: Option[String],
  httpMethod: String,
  httpStatus: Int,
  sessionId: String,
  who: Option[Int],
  timeAtStart: Long,
  what: String,
  elapsed: Int,
  uuid: String
) extends BaseHttpAuditEvent {
  override val auditType = HttpAuditEvent.auditType
}

object HttpAuditEvent extends AuditLogFieldExtractors {

  val auditType = "HTTP"

  def apply(jsEvent: JValue): Try[HttpAuditEvent] = Try {
    HttpAuditEvent(
      accessPath(jsEvent),
      httpMethod(jsEvent),
      httpStatus(jsEvent),
      sessionId(jsEvent),
      who(jsEvent),
      timeAtStart(jsEvent),
      what(jsEvent),
      elapsed(jsEvent),
      uuid(jsEvent)
    )
  }
}

final case class ExtendedHttpAuditEvent(
  accessPath: Option[String],
  httpMethod: String,
  httpStatus: Int,
  sessionId: String,
  who: Option[Int],
  timeAtStart: Long,
  what: String,
  elapsed: Int,
  uuid: String
) extends BaseHttpAuditEvent {
  override val auditType = ExtendedHttpAuditEvent.auditType
}

object ExtendedHttpAuditEvent extends AuditLogFieldExtractors {
  val auditType = "EXTENDED_HTTP"

  def apply(jsEvent: JValue): Try[ExtendedHttpAuditEvent] = Try {
    ExtendedHttpAuditEvent(
      accessPath(jsEvent),
      httpMethod(jsEvent),
      httpStatus(jsEvent),
      sessionId(jsEvent),
      who(jsEvent),
      timeAtStart(jsEvent),
      what(jsEvent),
      elapsed(jsEvent),
      uuid(jsEvent)
    )
  }
}

object AuditEvent {
  implicit private val formats = DefaultFormats

  def parseRaw(json: String): Try[AuditEvent] = {
    Try(parse(json)).flatMap { jsValueEvent =>
      jsValueEvent \ "type" match {
        case JString(eventType) if eventType == "HttpAuditEvent" => HttpAuditEvent(jsValueEvent \ "event")
        case JString(eventType) if eventType == "ExtendedHttpAuditEvent" => ExtendedHttpAuditEvent(jsValueEvent \"event")
        case JString(eventType) if eventType == "SshAuditEvent" => SshAuditEvent(jsValueEvent \ "event")
        case _ => Failure(new MappingException(s"Could not parse json into an audit event: $json"))
      }
    }
  }
}