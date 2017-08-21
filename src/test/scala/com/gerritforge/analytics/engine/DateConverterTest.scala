package com.gerritforge.analytics.engine

import com.gerritforge.analytics.engine.GerritAnalyticsTrasformations._
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.native.JsonMethods.{compact, render}
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{FlatSpec, Inside, Matchers}

class DateConverterTest extends FlatSpec with Matchers with Inside {

  "converter" should "convert 3 canonical numbers in a nested json object" in {
    val DATES = Map(
      0L -> "1970-01-01T00:00:00Z",
      1500000000000L -> "2017-07-14T02:40:00Z",
      1600000000000L -> "2020-09-13T12:26:40Z")
    val json =
      """{
         "date": 0,
         "name": "foo",
         "commits": [
            {"sha1": "xxx", "last_commit_date": 1500000000000},
            {"sha1": "yyy", "last_commit_date": 1600000000000}
          }
      }"""
    val out = transformLongDateToISO(json)
    inside (out \ "date") { case JString(s0) => s0 should equal(DATES(0L)) }
    inside (out \ "commits") { case JArray(List(o1,o2)) =>
      inside (o1 \ "last_commit_date") { case JString(s15) => s15 should equal(DATES(1500000000000L))}
      inside (o2 \ "last_commit_date") { case JString(s16) => s16 should equal(DATES(1600000000000L))}
    }
  }
}
