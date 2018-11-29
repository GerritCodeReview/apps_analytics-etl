package com.gerritforge.analytics.auditlog.model
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.MappingException
import org.json4s.ParserUtil.ParseException
import org.json4s.native.JsonMethods._
import org.scalatest.TryValues._
import org.scalatest.{FlatSpec, Inside, Matchers}

class AuditEventSpec extends FlatSpec with Matchers with Inside {

  behavior of "parseRaw"

  "parsing a string that is not json" should "result in a ParseException failure" in {
    val someJson = """this_is_not_a_valid_json"""
    AuditEvent.parseRaw(someJson).failure.exception shouldBe a[ParseException]
  }

  "parsing a json not representing an audit event log" should "result in a MappingException failure" in {
    val someJson = """{"some": "json", "not": "expected"}"""

    AuditEvent.parseRaw(someJson).failure.exception shouldBe a[MappingException]
  }

  "A json representing an http audit event log" should "be parsed into an audit event" in {

    val httpMethod = "POST"
    val httpStatus = 200
    val sessionId  = "1r7ywi4vd3jk410dv60pvd19vk"
    val accountId  = 1009124
    val accessPath = "GIT"
    val timeAtStart       = 1542240322369L
    val what       = "https://review.gerrithub.io/Mirantis/tcp-qa/git-upload-pack"
    val elapsed    = 12
    val auditUUID = "audit:fe4cff68-d094-474a-9d97-502270b0b2e6"
    val jsonEvent =
      s"""
         |{
         |  "type": "HttpAuditEvent",
         |  "event": {
         |    "http_method": "$httpMethod",
         |    "http_status": $httpStatus,
         |    "session_id": "$sessionId",
         |    "who": {
         |      "account_id": {
         |        "id": $accountId
         |      },
         |      "real_user": {
         |        "account_id": {
         |          "id": $accountId
         |        },
         |        "access_path": "UNKNOWN",
         |        "last_login_external_id_property_key": {}
         |      },
         |      "access_path": "$accessPath",
         |      "last_login_external_id_property_key": {}
         |    },
         |    "when": $timeAtStart,
         |    "what": "$what",
         |    "params": {},
         |    "time_at_start": $timeAtStart,
         |    "elapsed": $elapsed,
         |    "uuid": {
         |      "uuid": "$auditUUID"
         |    }
         |  }
         |}
       """.stripMargin

    val triedEvent = AuditEvent.parseRaw(jsonEvent)
    inside (triedEvent.success.value) {
      case AuditEvent(gotSessionId, gotWho, gotAccessPath, gotTimeAtStart, gotHttpMethod, gotWhat, gotElapsed, gotUUID, gotAuditType) =>
        gotSessionId   shouldBe sessionId
        gotWho         should contain(accountId)
        gotTimeAtStart shouldBe timeAtStart
        gotHttpMethod  should contain(httpMethod)
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
        gotUUID        shouldBe auditUUID
        gotAccessPath  should contain(accessPath)
        gotAuditType   shouldBe "HttpAuditEvent"
    }
  }

  "A json representing a SSH audit event log" should "be parsed into a success of SshAuditEvent" in {

    val sessionId   = "2adc5bef"
    val accountId   = 1009124
    val accessPath  = "SSH_COMMAND"
    val timeAtStart = 1542240322369L
    val what        = "gerrit.stream-events.-s.patchset-created.-s.change-restored.-s.comment-added"
    val elapsed     = 12
    val auditUUID   = "audit:dd74e098-9260-4720-9143-38a0a0a5e500"
    val jsonEvent =
      s"""
         |{
         |  "type": "SshAuditEvent",
         |  "event": {
         |    "session_id": "$sessionId",
         |    "who": {
         |      "account_id": {
         |        "id": $accountId
         |      },
         |      "access_path": "$accessPath",
         |      "last_login_external_id_property_key": {}
         |    },
         |    "when": $timeAtStart,
         |    "what": "$what",
         |    "params": {},
         |    "result": "0",
         |    "time_at_start": $timeAtStart,
         |    "elapsed": $elapsed,
         |    "uuid": {
         |      "uuid": "$auditUUID"
         |    }
         |  }
         |}
       """.stripMargin

    inside (AuditEvent.parseRaw(jsonEvent).success.value) {
      case AuditEvent(gotSessionId, gotWho, gotAccessPath, gotTimeAtStart, gotHttpMethod, gotWhat, gotElapsed, gotUUID, gotAuditType) =>
        gotSessionId   shouldBe sessionId
        gotWho         should contain(accountId)
        gotTimeAtStart shouldBe timeAtStart
        gotHttpMethod  shouldBe empty
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
        gotUUID        shouldBe auditUUID
        gotAccessPath  should contain(accessPath)
        gotAuditType   shouldBe "SshAuditEvent"
    }
  }

  "A json representing a failed SSH authentication failure" should "be parsed into an AuditEvent" in {

    val sessionId   = "000000000000000000000000000"
    val timeAtStart = 1542240154088L
    val what        = "AUTH"
    val elapsed     = 0
    val auditUUID   = "audit:8d40c495-7b51-4003-81f2-718bc04addf3"
    val jsonEvent =
      s"""
         |{
         |  "type": "SshAuditEvent",
         |  "event": {
         |    "session_id": "$sessionId",
         |    "when": 1542240154088,
         |    "what": "$what",
         |    "params": {},
         |    "result": "FAIL",
         |    "time_at_start": $timeAtStart,
         |    "elapsed": $elapsed,
         |    "uuid": {
         |      "uuid": "$auditUUID"
         |    }
         |  }
         |}
       """.stripMargin

    inside (AuditEvent.parseRaw(jsonEvent).success.value) {
      case AuditEvent(gotSessionId, gotWho, gotAccessPath, gotTimeAtStart, gotHttpMethod, gotWhat, gotElapsed, gotUUID, gotAuditType) =>
        gotSessionId   shouldBe sessionId
        gotWho         shouldBe empty
        gotTimeAtStart shouldBe timeAtStart
        gotHttpMethod  shouldBe empty
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
        gotUUID        shouldBe auditUUID
        gotAccessPath  shouldBe empty
        gotAuditType   shouldBe "SshAuditEvent"
    }
  }

  "A json representing an extended http audit event log" should "be parsed into an ExtendedHttpAuditEvent" in {

    val httpMethod  = "GET"
    val httpStatus  = 200
    val sessionId   = "aQ3Dprttdq3tT25AMDHhF7zKpMOph64XnW"
    val accountId   = 1011373
    val accessPath  = "REST_API"
    val timeAtStart = 1542242297450L
    val what        = "/config/server/info"
    val elapsed     = 177
    val auditUUID   = "audit:5f10fea5-35d1-4252-b86f-99db7a9b549b"
    val jsonEvent   =
      s"""
         |{
         |  "type": "ExtendedHttpAuditEvent",
         |  "event": {
         |    "http_method": "$httpMethod",
         |    "http_status": $httpStatus,
         |    "session_id": "$sessionId",
         |    "who": {
         |      "account_id": {
         |        "id": $accountId
         |      },
         |      "access_path": "$accessPath",
         |      "last_login_external_id_property_key": {}
         |    },
         |    "when": 1542242297450,
         |    "what": "$what",
         |    "params": {},
         |    "time_at_start": $timeAtStart,
         |    "elapsed": $elapsed,
         |    "uuid": {
         |      "uuid": "$auditUUID"
         |    }
         |  }
         |}
       """.stripMargin

    inside (AuditEvent.parseRaw(jsonEvent).success.value) {
      case AuditEvent(gotSessionId, gotWho, gotAccessPath, gotTimeAtStart, gotHttpMethod, gotWhat, gotElapsed, gotUUID, gotAuditType) =>
        gotSessionId   shouldBe sessionId
        gotWho         should   contain(accountId)
        gotTimeAtStart shouldBe timeAtStart
        gotHttpMethod  should contain(httpMethod)
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
        gotUUID        shouldBe auditUUID
        gotAccessPath  should   contain(accessPath)
        gotAuditType   shouldBe "ExtendedHttpAuditEvent"
    }
  }

  behavior of "toJsonString"

  "an HttpAuditEvent" should "be serializable into json" in {

    val httpAuditEvent = "HttpAuditEvent"
    val httpMethod = "GET"
    val sessionId = "someSessionId"
    val accessPath = "GIT"
    val timeAtStart = 1000L
    val elapsed = 12
    val what="https://review.gerrithub.io/Mirantis/tcp-qa/git-upload-pack"
    val uuid = "audit:5f10fea5-35d1-4252-b86f-99db7a9b549b"

    val event = AuditEvent(sessionId, None, Some(accessPath), timeAtStart, Some(httpMethod), what, elapsed, uuid, httpAuditEvent)

    val expectedJson: JValue =
      ("session_id" -> sessionId) ~
      ("access_path" -> accessPath) ~
      ("time_at_start" -> timeAtStart) ~
      ("http_method" -> httpMethod) ~
      ("what" -> what) ~
      ("elapsed" -> elapsed) ~
      ("uuid" -> uuid) ~
      ("audit_type" -> httpAuditEvent)

    parse(event.toJsonString) shouldBe expectedJson
  }

  "an ExtendedHttpAuditEvent" should "be serializable into json" in {

    val httpAuditEvent = "HttpAuditEvent"
    val httpMethod = "GET"
    val accessPath = "REST_API"
    val sessionId = "someSessionId"
    val accountId = 123
    val timeAtStart = 1000L
    val what="/config/server/info"
    val elapsed = 22
    val uuid = "audit:5f10fea5-35d1-4252-b86f-99db7a9b549b"

    val event = AuditEvent(sessionId, Some(accountId), Some(accessPath), timeAtStart, Some(httpMethod), what, elapsed, uuid, httpAuditEvent)

    val expectedJson: JValue =
      ("session_id" -> sessionId) ~
      ("who" -> accountId) ~
      ("access_path" -> accessPath) ~
      ("time_at_start" -> timeAtStart) ~
      ("http_method" -> httpMethod) ~
      ("what" -> what) ~
      ("elapsed" -> elapsed) ~
      ("uuid" -> uuid) ~
      ("audit_type" -> httpAuditEvent)

    parse(event.toJsonString) shouldBe expectedJson
  }

  "an SshAuditEvent" should "be serializable into json" in {
    val sshAuditEvent = "sshAuditEvent"
    val sessionId   = "2adc5bef"
    val accountId   = 1009124
    val accessPath  = "SSH_COMMAND"
    val timeAtStart = 1542240322369L
    val what        = "gerrit.stream-events.-s.patchset-created.-s.change-restored.-s.comment-added"
    val elapsed     = 12
    val uuid   = "audit:dd74e098-9260-4720-9143-38a0a0a5e500"

    val event = AuditEvent(sessionId, Some(accountId), Some(accessPath), timeAtStart, None, what, elapsed, uuid, sshAuditEvent)

    val expectedJson: JValue =
      ("session_id" -> sessionId) ~
      ("who" -> accountId) ~
      ("access_path" -> accessPath) ~
      ("time_at_start" -> timeAtStart) ~
      ("what" -> what) ~
      ("elapsed" -> elapsed) ~
      ("uuid" -> uuid) ~
      ("audit_type" -> sshAuditEvent)

    parse(event.toJsonString) shouldBe expectedJson
  }
}