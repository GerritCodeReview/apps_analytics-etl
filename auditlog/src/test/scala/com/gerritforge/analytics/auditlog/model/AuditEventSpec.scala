package com.gerritforge.analytics.auditlog.model
import org.json4s.MappingException
import org.json4s.ParserUtil.ParseException
import org.scalatest.{FlatSpec, Inside, Matchers}
import org.scalatest.TryValues._

class AuditEventSpec extends FlatSpec with Matchers with Inside {

  "parsing a string that is not json" should "result in a ParseException failure" in {
    val someJson = """this_is_not_a_valid_json"""
    AuditEvent.fromJsonString(someJson).failure.exception shouldBe a[ParseException]
  }

  "parsing a json not representing an audit event log" should "result in a MappingException failure" in {
    val someJson = """{"some": "json", "not": "expected"}"""

    AuditEvent.fromJsonString(someJson).failure.exception shouldBe a[MappingException]
  }

  "A json representing an http audit event log" should "be parsed into a success of HttpAuditEvent" in {

    val httpMethod = "POST"
    val httpStatus = 200
    val sessionId  = "1r7ywi4vd3jk410dv60pvd19vk"
    val accountId  = 1009124
    val accessPath = "GIT"
    val timeAtStart       = 1542240322369L
    val what       = "https://review.gerrithub.io/Mirantis/tcp-qa/git-upload-pack"
    val elapsed    = 12
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
         |      "uuid": "audit:fe4cff68-d094-474a-9d97-502270b0b2e6"
         |    }
         |  }
         |}
       """.stripMargin

    inside (AuditEvent.fromJsonString(jsonEvent).success.value) {
      case HttpAuditEvent(gotHttpMethod, gotHttpStatus, gotSessionId, gotWho, gotTimeAtStart, gotWhat, gotElapsed) =>
        gotHttpMethod  shouldBe httpMethod
        gotHttpStatus  shouldBe httpStatus
        gotSessionId   shouldBe sessionId
        gotWho         shouldBe CurrentUser(accessPath = accessPath, accountId = Some(AccountId(accountId)))
        gotTimeAtStart shouldBe timeAtStart
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
    }
  }

  "A json representing a SSH audit event log" should "be parsed into a success of SshAuditEvent" in {

    val sessionId   = "2adc5bef"
    val accountId   = 1009124
    val accessPath  = "SSH_COMMAND"
    val timeAtStart = 1542240322369L
    val what       = "gerrit.stream-events.-s.patchset-created.-s.change-restored.-s.comment-added"
    val elapsed    = 12
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
         |      "uuid": "audit:dd74e098-9260-4720-9143-38a0a0a5e500"
         |    }
         |  }
         |}
       """.stripMargin

    inside (AuditEvent.fromJsonString(jsonEvent).success.value) {
      case SshAuditEvent(gotSessionId, gotWho, gotTimeAtStart, gotWhat, gotElapsed) =>
        gotSessionId   shouldBe sessionId
        gotWho         shouldBe Some(CurrentUser(accessPath = accessPath, accountId = Some(AccountId(accountId))))
        gotTimeAtStart shouldBe timeAtStart
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
    }
  }

  "A json representing a failed SSH authentication failure" should "be parsed into a success of SshAuditEvent" in {

    val sessionId   = "000000000000000000000000000"
    val timeAtStart = 1542240154088L
    val what        = "AUTH"
    val elapsed     = 0
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
         |      "uuid": "audit:8d40c495-7b51-4003-81f2-718bc04addf3"
         |    }
         |  }
         |}
       """.stripMargin

    inside (AuditEvent.fromJsonString(jsonEvent).success.value) {
      case SshAuditEvent(gotSessionId, gotWho, gotTimeAtStart, gotWhat, gotElapsed) =>
        gotSessionId   shouldBe sessionId
        gotWho         shouldBe None
        gotTimeAtStart shouldBe timeAtStart
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
    }
  }

  "A json representing an extended http audit event log" should "be parsed into a success of ExtendedHttpAuditEvent" in {

    val httpMethod  = "GET"
    val httpStatus  = 200
    val sessionId   = "aQ3Dprttdq3tT25AMDHhF7zKpMOph64XnW"
    val accountId   = 1011373
    val accessPath  = "REST_API"
    val timeAtStart = 1542242297450L
    val what        = "/config/server/info"
    val elapsed     = 177
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
         |      "uuid": "audit:5f10fea5-35d1-4252-b86f-99db7a9b549b"
         |    }
         |  }
         |}
       """.stripMargin

    inside (AuditEvent.fromJsonString(jsonEvent).success.value) {
      case ExtendedHttpAuditEvent(gotHttpMethod, gotHttpStatus, gotSessionId, gotWho, gotTimeAtStart, gotWhat, gotElapsed) =>
        gotHttpMethod  shouldBe httpMethod
        gotHttpStatus  shouldBe httpStatus
        gotSessionId   shouldBe sessionId
        gotWho         shouldBe CurrentUser(accessPath = accessPath, accountId = Some(AccountId(accountId)))
        gotTimeAtStart shouldBe timeAtStart
        gotWhat        shouldBe what
        gotElapsed     shouldBe elapsed
    }
  }
}