package com.gerritforge.analytics.engine.events

import com.gerritforge.analytics.SparkTestSupport
import com.gerritforge.analytics.engine.events.GerritEventsTransformations.SerializableGerritEvent
import com.sonymobile.tools.gerrit.gerritevents.dto.GerritJsonEvent
import com.sonymobile.tools.gerrit.gerritevents.dto.events.{ChangeMerged, RefUpdated}
import org.apache.spark.rdd.RDD
import org.scalatest.{Matchers, WordSpec}

class GerritEventsTransformationsSpec extends WordSpec with Matchers with SparkTestSupport {

  "tryParseGerritTriggeredEvent" should {

    "Parse a correctly formed event" in new EventFixture {
      GerritEventsTransformations.tryParseGerritTriggeredEvent(refUpdated.json) shouldBe Right(refUpdated)
    }

    "Return the given string as Left object if the JSON provided is invalid" in {
      val invalidJson = "invalid json string"
      GerritEventsTransformations.tryParseGerritTriggeredEvent(invalidJson) shouldBe Left(invalidJson)
    }

    "Return the given string as Left object if the JSON event is not supported" in {
      val unsupportedEvent =
        """{"submitter":{"name":"Administrator","email":"admin@example.com","username":"admin"},
             "refUpdate":{"oldRev":"863b64002f2a9922deba69407804a44703c996e0",
             "newRev":"d3131be8d7c920badd28b70d8c039682568c8de5",
             "refName": "refs/heads/master","project":"subcut"},
            "type":"ref-updated-UNSUPPORTED","eventCreatedOn":1516531868}"""

      GerritEventsTransformations.tryParseGerritTriggeredEvent(unsupportedEvent) shouldBe Left(unsupportedEvent)
    }
  }

  "PimpedJsonRDD" should {
    "Convert an RDD of JSON events into an RDD of events or unparsed json strings" in new EventFixture {
      val jsonRdd = sc.parallelize(Seq(refUpdated.json, changeMerged.json, "invalid json string"))

      import GerritEventsTransformations._

      jsonRdd.parseEvents.collect() should contain only(
        Right(refUpdated),
        Right(changeMerged),
        Left("invalid json string")
      )
    }
  }

  "SerializableGerritEvent" should {

    "Allow events to be stored in RDDs and transformed without loosing track of the originating event type" in new EventFixture {
      private val eventSize = 10000
      val eventRdd: RDD[SerializableGerritEvent[GerritJsonEvent]] = sc.parallelize(
        (1 to eventSize).map(indx => aChangeMergedEvent(indx.toString)) ++ (1 to eventSize).map(indx => aRefUpdatedEvent(indx.toString, (indx + 1).toString)),
        4
      )

      // Forcing some shuffle and accessing the inner class by type
      val withSomeTransformations = eventRdd
        .groupBy(_.asEvent().getEventType).mapValues { events =>
          events.head.asEvent() match {
            case changeMerged: ChangeMerged =>
              events.map(_.asEvent().asInstanceOf[ChangeMerged].getChange.getId.toInt).toList.sorted

            case refUpdated: RefUpdated =>
              events.map(_.asEvent().asInstanceOf[RefUpdated].getRefUpdate.getNewRev.toInt).toList.sorted
          }
        }

      withSomeTransformations.collect() should contain only (
        (refUpdated.asEvent().getEventType, (2 to eventSize + 1).toList),
        (changeMerged.asEvent().getEventType, (1 to eventSize).toList)
      )
    }

  }
}

trait EventFixture {

  val refUpdated = aRefUpdatedEvent(oldRev = "863b64002f2a9922deba69407804a44703c996e0", newRev = "d3131be8d7c920badd28b70d8c039682568c8de5")

  val changeMerged = aChangeMergedEvent("I5e6b5a3bbe8a29fb0393e4a28da536e0a198b755")

  def aRefUpdatedEvent(oldRev: String, newRev: String) = SerializableGerritEvent[RefUpdated](
    s"""{"submitter":{"name":"Administrator","email":"admin@example.com","username":"admin"},
      | "refUpdate":{"oldRev":"$oldRev",
      | "newRev":"$newRev",
      | "refName": "refs/heads/master","project":"subcut"},
      |"type":"ref-updated","eventCreatedOn":1516531868}""".stripMargin)

  def aChangeMergedEvent(changeId: String) = SerializableGerritEvent[ChangeMerged](
    s"""{
      |"submitter":{"name":"Administrator","email":"admin@example.com","username":"admin"},
      |"newRev":"863b64002f2a9922deba69407804a44703c996e0",
      |"patchSet":{
      | "number":1,
      | "revision":"863b64002f2a9922deba69407804a44703c996e0",
      | "parents":["4a4e59272f1f88824d805c0f4233c1ee7331e986"],
      | "ref":"refs/changes/01/1/1",
      | "uploader":{"name":"Administrator","email":"admin@example.com","username":"admin"},
      | "createdOn":1516530259,
      | "author":{"name":"Stefano Galarraga","email":"galarragas@gmail.com","username":""},
      | "isDraft":false,
      | "kind":"REWORK",
      | "sizeInsertions":2,
      | "sizeDeletions":0
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
      |"eventCreatedOn":1516530277
      |}""".stripMargin)

}