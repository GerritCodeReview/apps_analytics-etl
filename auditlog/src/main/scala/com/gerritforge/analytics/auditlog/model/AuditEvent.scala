// Copyright (C) 2018 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
  def result: String

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
    uuid: String,
    result: String
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
      uuid(jsEvent),
      sshResult(jsEvent)
    )
  }
}

sealed trait BaseHttpAuditEvent extends AuditEvent {
  def httpMethod: String
}

final case class HttpAuditEvent(
    accessPath: Option[String],
    httpMethod: String,
    result: String,
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
      httpResult(jsEvent),
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
    result: String,
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
      httpResult(jsEvent),
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
        case JString(eventType) if eventType == "HttpAuditEvent" =>
          HttpAuditEvent(jsValueEvent \ "event")
        case JString(eventType) if eventType == "ExtendedHttpAuditEvent" =>
          ExtendedHttpAuditEvent(jsValueEvent \ "event")
        case JString(eventType) if eventType == "SshAuditEvent" =>
          SshAuditEvent(jsValueEvent \ "event")
        case _ => Failure(new MappingException(s"Could not parse json into an audit event: $json"))
      }
    }
  }
}
