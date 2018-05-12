package com.gerritforge.analytics.support.ops

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

import com.gerritforge.analytics.support.ops.AnalyticsTimeOps.AnalyticsDateTimeFormater
import org.scalatest.{FlatSpec, Matchers}

class AnalyticsTimeOpsSpec extends FlatSpec with Matchers {


  "String parser - Given a correct string and date format" should "return an epoch value" in {
    val epochValueUTC =
      LocalDateTime
        .of(2018, 1, 1, 12, 0, 0, 0)
        .atOffset(ZoneOffset.UTC)
        .toInstant
        .toEpochMilli

    val stringDate = "2018-01-01 12:00:00.000000000"
    val dateFormat = AnalyticsDateTimeFormater.yyyy_MM_dd_HHmmss_SSSSSSSSS

    import AnalyticsTimeOps.implicits._

    stringDate.parseStringToUTCEpoch(dateFormat).get should equal(epochValueUTC)
  }
  "String parser - Given a correct string and date format" should "return also a local date" in {
    val utcLocalDate: LocalDate =
      LocalDate.of(2018, 1, 1)

    val stringDate = "2018-01-01"
    val dateFormat = AnalyticsDateTimeFormater.yyyy_MM_dd

    import AnalyticsTimeOps.implicits._

    stringDate.parseStringToLocalDate(dateFormat).get should equal(utcLocalDate)
  }


  "String parser - An incorrect string a given format" should "return None" in {
    val stringDate = "2018-01-01 12:00:00.000000000"
    val dateFormat = AnalyticsDateTimeFormater.yyyy_MM_dd

    import AnalyticsTimeOps.implicits._
    stringDate.parseStringToUTCEpoch(dateFormat) should equal(None)
  }


  "Simple Date Formats" should "convert to the correct strings" in {

    val epochValueUTC =
      LocalDateTime
        .of(2018, 1, 1, 12, 0, 0, 0)
        .atOffset(ZoneOffset.UTC)
        .toInstant.toEpochMilli

    val yyyyMMddHHStr = "2018010112"
    val yyyyMMddStr = "20180101"
    val yyyyMMStr = "201801"
    val yyyyStr = "2018"

    AnalyticsDateTimeFormater.yyyyMMddHH.format(epochValueUTC) should equal(yyyyMMddHHStr)
    AnalyticsDateTimeFormater.yyyyMMdd.format(epochValueUTC) should equal(yyyyMMddStr)
    AnalyticsDateTimeFormater.yyyyMM.format(epochValueUTC) should equal(yyyyMMStr)
    AnalyticsDateTimeFormater.yyyy.format(epochValueUTC) should equal(yyyyStr)
  }


  "UTC conversion" should "check date operations return always UTC" in {
    val dateTime =
      LocalDateTime
        .of(2018, 1, 1, 12, 0, 0, 0)

    val etcDateTime = dateTime.atOffset(ZoneOffset.ofHours(9))
    val utcDateTime = dateTime.atOffset(ZoneOffset.UTC)

    import AnalyticsTimeOps.implicits._
    dateTime.convertToUTCEpochMillis should equal(utcDateTime.toInstant.toEpochMilli)
    dateTime.convertToUTCEpochMillis should not equal (etcDateTime.toInstant.toEpochMilli)

    dateTime.convertToUTCLocalDateTime should equal(utcDateTime)
    dateTime.convertToUTCLocalDateTime should not equal (etcDateTime)

  }
}
