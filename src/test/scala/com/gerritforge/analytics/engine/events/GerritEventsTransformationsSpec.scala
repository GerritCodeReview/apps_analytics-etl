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

package com.gerritforge.analytics.engine.events

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField.{MILLI_OF_SECOND, NANO_OF_SECOND}
import java.time.{ZoneId, ZonedDateTime}

import com.gerritforge.analytics.engine.GerritAnalyticsTransformations.{CommitInfo, UserActivitySummary}
import com.gerritforge.analytics.engine.events.GerritEventsTransformations.NotParsableJsonEvent
import com.gerritforge.analytics.support.{EventFixture, SparkTestSupport}
import org.apache.spark.rdd.RDD
import org.scalatest.{Inside, Matchers, WordSpec}

class GerritEventsTransformationsSpec extends WordSpec with Matchers with SparkTestSupport with Inside with EventFixture {

  "tryParseGerritTriggeredEvent" should {

    implicit val eventParser: GerritJsonEventParser = EventParser

    "Parse a correctly formed event" in new EventFixture {
      GerritEventsTransformations.tryParseGerritTriggeredEvent(refUpdated.json) shouldBe Right(refUpdated.event)
    }

    "Return a description of the failure in with the original event source a Left object if the JSON provided is invalid" in {
      val invalidJson = "invalid json string"
      GerritEventsTransformations.tryParseGerritTriggeredEvent(invalidJson) shouldBe Left(NotParsableJsonEvent(invalidJson, "unknown token i - Near: i"))
    }

    "Return a description of the failure with the original event source  in a Left object if the JSON event is not supported" in {
      val unsupportedEvent = """{"type":"ref-updated-UNSUPPORTED","eventCreatedOn":1516531868}"""

      GerritEventsTransformations.tryParseGerritTriggeredEvent(unsupportedEvent) shouldBe Left(NotParsableJsonEvent(unsupportedEvent, "Unsupported event type 'ref-updated-UNSUPPORTED'"))
    }
  }

  "PimpedJsonRDD" should {
    "Convert an RDD of JSON events into an RDD of events or unparsed json strings" in {
      val jsonRdd = sc.parallelize(Seq(refUpdated.json, changeMerged.json, "invalid json string"))

      import GerritEventsTransformations._

      jsonRdd.parseEvents(EventParser).collect() should contain only(
        Right(refUpdated.event),
        Right(changeMerged.event),
        Left(NotParsableJsonEvent("invalid json string", "unknown token i - Near: i"))
      )
    }
  }

  "extractUserActivitySummary" should {
    implicit val eventParser: GerritJsonEventParser = EventParser

    "Build one UserActivitySummary object if given a series of non-merge commits" in {
      val events: Seq[ChangeMergedEvent] = Seq(
        aChangeMergedEvent("1", 1001l, newRev = "rev1", insertions = 2, deletions = 1),
        aChangeMergedEvent("2", 1002l, newRev = "rev2", insertions = 3, deletions = 0),
        aChangeMergedEvent("3", 1003l, newRev = "rev3", insertions = 1, deletions = 4),
        aChangeMergedEvent("4", 1004l, newRev = "rev4", insertions = 0, deletions = 2),
        aChangeMergedEvent("5", 1005l, newRev = "rev5", insertions = 1, deletions = 1)
      ).map(_.event)

      val summaries: Iterable[UserActivitySummary] = GerritEventsTransformations.extractUserActivitySummary(
        changes = events,
        year = 2018, month = 1, day = 10, hour = 1
      )

      summaries should have size 1

      inside(summaries.head) {
        case UserActivitySummary(year, month, day, hour, name, email, num_commits, _, _, added_lines, deleted_lines, commits, last_commit_date, is_merge) =>
          year shouldBe 2018
          month shouldBe 1
          day shouldBe 10
          hour shouldBe 1
          name shouldBe "Administrator"
          email shouldBe "admin@example.com"
          num_commits shouldBe events.size
          last_commit_date shouldBe 1005000l
          is_merge shouldBe false
          added_lines shouldBe 7
          deleted_lines shouldBe 8
          commits should contain only(
            CommitInfo("rev1", 1001000l, false),
            CommitInfo("rev2", 1002000l, false),
            CommitInfo("rev3", 1003000l, false),
            CommitInfo("rev4", 1004000l, false),
            CommitInfo("rev5", 1005000l, false)
          )
      }
    }

    "Build two UserActivitySummaries object if given a mixed series of merge and non merge commits" in {
      val events: Seq[ChangeMergedEvent] = Seq(
        aChangeMergedEvent("1", 1001l, newRev = "rev1", isMergeCommit = true),
        aChangeMergedEvent("2", 1002l, newRev = "rev2"),
        aChangeMergedEvent("3", 1003l, newRev = "rev3", isMergeCommit = true),
        aChangeMergedEvent("4", 1004l, newRev = "rev4"),
        aChangeMergedEvent("5", 1005l, newRev = "rev5")
      ).map(_.event)

      val summaries: Iterable[UserActivitySummary] = GerritEventsTransformations.extractUserActivitySummary(
        changes = events,
        year = 2018, month = 1, day = 10, hour = 1
      )

      summaries should have size 2

      summaries.foreach { summary =>
        inside(summary) {
          case UserActivitySummary(year, month, day, hour, name, email, _, _, _, _, _, _, _, _) =>
            year shouldBe 2018
            month shouldBe 1
            day shouldBe 10
            hour shouldBe 1
            name shouldBe "Administrator"
            email shouldBe "admin@example.com"
        }
      }

      summaries.foreach { summary =>
        inside(summary) {
          case UserActivitySummary(_, _, _, _, _, _, num_commits, _, _, _, _, commits, last_commit_date, false) =>
            num_commits shouldBe 3
            last_commit_date shouldBe 1005000l
            commits should contain only(
              CommitInfo("rev2", 1002000l, false),
              CommitInfo("rev4", 1004000l, false),
              CommitInfo("rev5", 1005000l, false)
            )

          case UserActivitySummary(_, _, _, _, _, _, num_commits, _, _, _, _, commits, last_commit_date, true) =>
            num_commits shouldBe 2
            last_commit_date shouldBe 1003000l
            commits should contain only(
              CommitInfo("rev1", 1001000l, true),
              CommitInfo("rev3", 1003000l, true)
            )
        }
      }

    }
  }

  "Pimped Per Project UserActivitySummary RDD" should {
    "Allow conversion to a DataFrame equivalent to what extracted from the Analytics plugin" in {
      import GerritEventsTransformations._
      import com.gerritforge.analytics.engine.GerritAnalyticsTransformations._
      import spark.implicits._

      val aliasDF = sc.parallelize(Seq(
        ("stefano_alias", "stefano@galarraga-org.com", "")
      )).toDF("author", "email", "organization")

      val expectedDate: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")).`with`(MILLI_OF_SECOND, 0).`with`(NANO_OF_SECOND, 0)

      val analyticsJobOutput =
        sc.parallelize(Seq(
          "project1" -> UserActivitySummary(2018, 1, 20, 10, "Stefano", "stefano@galarraga-org.com", 1, 2, 1, 10, 4, Array(CommitInfo("sha1", expectedDate.toInstant.toEpochMilli, false)),
            expectedDate.toInstant.toEpochMilli, false)
        ))
          .asEtlDataFrame(sql)
          .addOrganization()
          .handleAliases(Some(aliasDF))
          .convertDates("last_commit_date")
          .dropCommits

      val expected = sc.parallelize(Seq(
        ("project1", "stefano_alias", "stefano@galarraga-org.com", 2018, 1, 20, 10, 2, 1, 10, 4, 1, expectedDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), false, "galarraga-org")
      )).toDF("project", "author", "email", "year", "month", "day", "hour", "num_files", "num_distinct_files",
        "added_lines", "deleted_lines", "num_commits", "last_commit_date", "is_merge", "organization")

      analyticsJobOutput.collect() should contain theSameElementsAs expected.collect()
    }
  }

  "removeEventsForCommits" should {
    "remove any event modifying a gerrit repo to go toward the commits to be excluded" in {
      import GerritEventsTransformations._

      val toKeep1 = aRefUpdatedEvent("oldRev", "RevToKeep")
      val toKeep2 = aChangeMergedEvent(changeId = "changeId2", newRev = "RevToKeep2")
      val events: RDD[GerritRefHasNewRevisionEvent] = sc.parallelize(Seq(
        aRefUpdatedEvent("oldRev", "RevToExclude1"),
        toKeep1,
        aRefUpdatedEvent("oldRev", "RevToExclude2"),
        aChangeMergedEvent(changeId = "changeId1", newRev = "RevToExclude3"),
        toKeep2
      ).map(_.event))

      events
        .removeEventsForCommits(sc.parallelize(Seq("RevToExclude1", "RevToExclude2", "RevToExclude3")))
        .collect() should contain only(toKeep1.event, toKeep2.event)
    }
  }

}