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


package com.gerritforge.analytics.support.ops

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}

package AnalyticsTimeOps {

  import java.sql.Timestamp
  import java.text.SimpleDateFormat
  import java.time.{Instant, LocalDate, OffsetDateTime}
  import java.util.TimeZone

  import scala.util.Try

  object AnalyticsDateTimeFormater {

    val yyyy_MM_dd_HHmmss_SSSSSSSSS: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")
    val yyyy_MM_dd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val yyyyMMddHH: SimpleDateFormat = buildSimpleDateFormatUTC("yyyyMMddHH")
    val yyyyMMdd: SimpleDateFormat= buildSimpleDateFormatUTC("yyyyMMdd")
    val yyyyMM: SimpleDateFormat = buildSimpleDateFormatUTC("yyyyMM")
    val yyyy: SimpleDateFormat = buildSimpleDateFormatUTC("yyyy")

    private def buildSimpleDateFormatUTC(pattern: String): SimpleDateFormat =  {
      val simpleDateFormat = new SimpleDateFormat(pattern)
      simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
      simpleDateFormat
    }
  }

  object CommonTimeOperations {
    def nowEpoch: Long = Instant.now().getEpochSecond

    def epochToSqlTimestampOps(epoch: Long) = new Timestamp(epoch)

    def nowSqlTimestmap: Timestamp = epochToSqlTimestampOps(nowEpoch)

    def utcDateTimeFromEpoch(epoch: Long): LocalDateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC)
  }

  object implicits {

    implicit class LocalDateTimeOps(val localDateTime: LocalDateTime) extends AnyVal {
      def convertToUTCEpochMillis: Long = localDateTime.atOffset(ZoneOffset.UTC).toInstant.toEpochMilli

      def convertToUTCLocalDateTime: OffsetDateTime = localDateTime.atOffset(ZoneOffset.UTC)
    }


    implicit class StringToTimeParsingOps(val dateStr: String) extends AnyVal {
      def parseStringToUTCEpoch(stringFormat: DateTimeFormatter): Option[Long] =
        Try(LocalDateTime.parse(dateStr, stringFormat).convertToUTCEpochMillis).toOption

      def parseStringToLocalDate(stringFormat: DateTimeFormatter): Option[LocalDate] =
        Try(LocalDate.parse(dateStr, stringFormat)).toOption
    }

  }

  trait DateConversions {
    val NO_TIMESTAMP = new Timestamp(0L)

    implicit def timestampToLocalDate(timestamp: Timestamp): Option[LocalDate] = timestamp match {
      case NO_TIMESTAMP => None
      case ts => Some(ts.toLocalDateTime.toLocalDate)
    }

    implicit def nullableStringToOption(nullableString: String): Option[String] = Option(nullableString)
  }
}