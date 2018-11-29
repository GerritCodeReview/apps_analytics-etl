package com.gerritforge.analytics.auditlog.range
import java.time.{Instant, LocalDate}
import com.gerritforge.analytics.support.ops.implicits._

import org.scalatest.{FlatSpec, Inside, Matchers}
import TimeRangeSpec._

class TimeRangeSpec extends FlatSpec with Matchers with Inside {
  behavior of "isWithin"

  it should "always return true when time range is boundless" in {
    val range = TimeRange(None, None)

    range.isWithin(nowMs) shouldBe true
  }

  it should "return true when 'until' is unbounded and time is greater than 'since'" in {
    val range = TimeRange(Some(yesterday), None)

    range.isWithin(nowMs) shouldBe true
  }

  it should "return false when 'until' is unbounded and time is less than 'since'" in {
    val range = TimeRange(Some(now), None)

    range.isWithin(yesterdayMs) shouldBe false
  }

  it should "return true when 'since' is unbounded and time is less than 'until'" in {
    val range = TimeRange(None, Some(tomorrow))

    range.isWithin(nowMs) shouldBe true
  }

  it should "return false when 'since' is unbounded and time is greater than 'until'" in {
    val range = TimeRange(None, Some(now))

    range.isWithin(tomorrowMs) shouldBe false
  }

  it should "return true when time is within bounded range" in {
    val range = TimeRange(Some(yesterday), Some(tomorrow))

    range.isWithin(nowMs) shouldBe true
  }
}

object TimeRangeSpec {
  val yesterday: LocalDate = LocalDate.now().minusDays(1)
  val tomorrow: LocalDate = LocalDate.now().plusDays(1)
  val now: LocalDate = LocalDate.now()
  val nowMs: Long = Instant.now().toEpochMilli
  val yesterdayMs = yesterday.atStartOfDay().convertToUTCEpochMillis
  val tomorrowMs = tomorrow.atStartOfDay().convertToUTCEpochMillis

}