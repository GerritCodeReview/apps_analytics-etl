package com.gerritforge.analytics.auditlog.range
import java.time.LocalDate

import com.gerritforge.analytics.support.ops.implicits._
import com.google.inject.Singleton

@Singleton
case class TimeRange(since: Option[LocalDate], until: Option[LocalDate]) {

  private val maybeSinceMs: Option[Long] = since.map(_.atStartOfDay().convertToUTCEpochMillis)
  private val maybeUntilMs: Option[Long] = until.map(_.atStartOfDay().convertToUTCEpochMillis)

  def isWithin(timeMs: Long): Boolean = maybeSinceMs.forall(_ <= timeMs) && maybeUntilMs.forall(_ >= timeMs)
}
