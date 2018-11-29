package com.gerritforge.analytics.support.ops
import java.time.LocalDate

import scopt.Read
import scopt.Read.reads

import scala.util.control.NonFatal

object ReadsOps {

  implicit val localDateRead: Read[LocalDate] = reads { dateStr =>
    val cliDateFormat = AnalyticsDateTimeFormatter.yyyy_MM_dd
    try {
      import com.gerritforge.analytics.support.ops.implicits._
      dateStr.parseStringToLocalDate(cliDateFormat).get
    } catch {
      case NonFatal(e) =>
        throw new IllegalArgumentException(
          s"Invalid date '$dateStr' expected format is '${cliDateFormat}'", e)
    }
  }
}
