package com.gerritforge.analytics.auditlog.model
import org.json4s.jackson.Serialization.write
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.util.{Failure, Success, Try}

object AuditEvent {

  implicit private val formats = DefaultFormats + AuditUUID.serializer + AccountId.serializer

  def fromJsonString(json: String): Try[AuditEvent] = {
    Try(parse(json)).flatMap { jsValueEvent =>
      jsValueEvent \ "type" match {
        case JString(eventType) if eventType == "HttpAuditEvent" =>
          Success((jsValueEvent \ "event").camelizeKeys.extract[HttpAuditEvent])
        case JString(eventType) if eventType == "ExtendedHttpAuditEvent" =>
          Success((jsValueEvent \ "event").camelizeKeys.extract[ExtendedHttpAuditEvent])
        case JString(eventType) if eventType == "SshAuditEvent" =>
          Success((jsValueEvent \ "event").camelizeKeys.extract[SshAuditEvent])
        case _ => Failure(new MappingException(s"Could not parse json into an audit event: $json"))
      }
    }
  }

  def toJsonString[T <: AuditEvent](auditEvent: T): String = {
    compact(render(parse(write[T](auditEvent)).snakizeKeys))
  }
}

sealed trait AuditEvent {
  def sessionId: String
  def timeAtStart: Long
  def what: String
  def elapsed: Int
  def uuid: AuditUUID
}

final case class SshAuditEvent(
  sessionId: String,
  who: Option[CurrentUser],
  timeAtStart: Long,
  what: String,
  elapsed: Int,
  uuid: AuditUUID
) extends AuditEvent

sealed trait BaseHttpAuditEvent extends AuditEvent {
  def httpMethod: String
  def httpStatus: Int
  def who: CurrentUser
}

final case class HttpAuditEvent(
    httpMethod: String,
    httpStatus: Int,
    sessionId: String,
    who: CurrentUser,
    timeAtStart: Long,
    what: String,
    elapsed: Int,
    uuid: AuditUUID
) extends BaseHttpAuditEvent

final case class ExtendedHttpAuditEvent(
    httpMethod: String,
    httpStatus: Int,
    sessionId: String,
    who: CurrentUser,
    timeAtStart: Long,
    what: String,
    elapsed: Int,
    uuid: AuditUUID
) extends BaseHttpAuditEvent

final case class CurrentUser(
  accessPath: String,
  accountId: Option[AccountId]
)

final case class AccountId(id: Int)
object AccountId {
  val serializer = new CustomSerializer[AccountId]( _ =>
      (
        { case JObject(JField("id", JInt(id)) :: Nil) => AccountId(id.intValue()) },
        { case a: AccountId => JInt(a.id) }
      )
  )
}

final case class AuditUUID(uuid: String)

object AuditUUID {
  val serializer = new CustomSerializer[AuditUUID]( _ =>
      (
        { case JObject(JField("uuid", JString(uuid)) :: Nil) => AuditUUID(uuid) },
        { case a: AuditUUID => JString(a.uuid) }
      )
  )
}