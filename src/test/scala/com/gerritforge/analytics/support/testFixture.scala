package com.gerritforge.analytics.support

import com.gerritforge.analytics.engine.events.{ChangeMergedEvent, EventParser, GerritJsonEvent, RefUpdatedEvent}

trait AnalyticPluginFixture {

  val anResponseFromAnalyticsPlugin =
    """{
      |  "name": "The User",
      |  "email": "the.user@company.com",
      |  "num_commits": 5,
      |  "num_files": 6,
      |  "num_distinct_files": 2,
      |  "added_lines": 7,
      |  "deleted_lines": 4,
      |  "commits": [
      |    {
      |      "sha1": "06acddab876fa4f0af5e92ef38ee797ccabc435f",
      |      "date": 1526052064000,
      |      "merge": false,
      |      "files": [
      |        "hello2.txt"
      |      ]
      |    },
      |    {
      |      "sha1": "51f5757d7489b52c2b317aebc9453af2aea7fe78",
      |      "date": 1526052154000,
      |      "merge": false,
      |      "files": [
      |        "hello2.txt"
      |      ]
      |    },
      |    {
      |      "sha1": "976821ca2d1324a98a1b30a035456616d5307b05",
      |      "date": 1523960215000,
      |      "merge": false,
      |      "files": [
      |        "hello.txt",
      |        "hello2.txt"
      |      ]
      |    },
      |    {
      |      "sha1": "02d2787b3862aced9ac13c82b85be98db8fb6900",
      |      "date": 1523959841000,
      |      "merge": false,
      |      "files": [
      |        "hello2.txt"
      |      ]
      |    },
      |    {
      |      "sha1": "f2dd6d322655ce7f28a937f85dcec93d69b98a66",
      |      "date": 1523959613000,
      |      "merge": false,
      |      "files": [
      |        "hello.txt"
      |      ]
      |    }
      |  ],
      |  "branches": [
      |
      |  ],
      |  "issues_codes": [
      |
      |  ],
      |  "issues_links": [
      |
      |  ],
      |  "last_commit_date": 1526052154000,
      |  "is_merge": false
      |}""".stripMargin
}

trait EventFixture {

  // Forcing early type failures
  case class JsonEvent[T <: GerritJsonEvent](json: String) {
    val event: T = EventParser.fromJson(json).get.asInstanceOf[T]
  }

  val refUpdated: JsonEvent[RefUpdatedEvent] = aRefUpdatedEvent(oldRev = "863b64002f2a9922deba69407804a44703c996e0", newRev = "d3131be8d7c920badd28b70d8c039682568c8de5")

  val changeMerged: JsonEvent[ChangeMergedEvent] = aChangeMergedEvent("I5e6b5a3bbe8a29fb0393e4a28da536e0a198b755")

  def aRefUpdatedEvent(oldRev: String, newRev: String, createdOn: Long = 1000l) = JsonEvent[RefUpdatedEvent](
    s"""{"submitter":{"name":"Administrator","email":"admin@example.com","username":"admin"},
       | "refUpdate":{"oldRev":"$oldRev",
       | "newRev":"$newRev",
       | "refName": "refs/heads/master","project":"subcut"},
       |"type":"ref-updated","eventCreatedOn":$createdOn}""".stripMargin)

  def aChangeMergedEvent(changeId: String, createdOnInSecs: Long = 1000l, newRev: String = "863b64002f2a9922deba69407804a44703c996e0",
                         isMergeCommit: Boolean = false, insertions: Integer = 0, deletions: Integer = 0) = JsonEvent[ChangeMergedEvent](
    s"""{
       |"submitter":{"name":"Administrator","email":"admin@example.com","username":"admin"},
       |"newRev":"$newRev",
       |"patchSet":{
       | "number":1,
       | "revision":"$newRev",
       | "parents": ${if (isMergeCommit) """["4a4e59272f1f88824d805c0f4233c1ee7331e986", "4a4e59272f1f88824d805c0f4233c1ee7331e987"]""" else """["4a4e59272f1f88824d805c0f4233c1ee7331e986"]"""},
       | "ref":"refs/changes/01/1/1",
       | "uploader":{"name":"Administrator","email":"admin@example.com","username":"admin"},
       | "createdOn":1516530259,
       | "author":{"name":"Stefano Galarraga","email":"galarragas@gmail.com","username":""},
       | "isDraft":false,
       | "kind":"REWORK",
       | "sizeInsertions":$insertions,
       | "sizeDeletions":$deletions
       |},
       |"change":{
       | "project":"subcut","branch":"master","topic":"TestEvents","id":"$changeId","number":1,"subject":"Generating some changes to test events",
       | "owner":{"name":"Administrator","email":"admin@example.com","username":"admin"},
       | "url":"http://842860da5b33:8080/1","commitMessage":"Generating some changes to test events Change-Id: $changeId",
       | "createdOn":1516530259,"status":"MERGED"
       |},
       |"project":{"name":"subcut"},
       |"refName":"refs/heads/master",
       |"changeKey":{"id":"$changeId"},
       |"type":"change-merged",
       |"eventCreatedOn": $createdOnInSecs
       |}""".stripMargin)

}