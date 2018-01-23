// Copyright (C) 2017 GerritForge Ltd
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

import java.text.{DateFormat, SimpleDateFormat}
import java.time.{LocalDateTime, ZoneOffset}

import scala.util.Try

sealed trait AggregationStrategy extends Serializable {
  def aggregationKey(event: GerritJsonEvent): String

  case class DateTimeParts(year: Integer, month: Integer, day: Integer, hour: Integer)

  def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): DateTimeParts = {
    val date = LocalDateTime.ofEpochSecond(event.eventCreatedOn, 0, ZoneOffset.UTC)
    DateTimeParts(date.getYear, date.getMonthValue, date.getDayOfMonth, date.getHour)
  }
}

object AggregationStrategy {

  @throws(classOf[IllegalArgumentException])
  def byName(aggregationName: String): Try[AggregationStrategy] = Try {
    aggregationName.toUpperCase match {
      case "EMAIL" => aggregateByEmail
      case "EMAIL_YEAR" => aggregateByEmailAndYear
      case "EMAIL_MONTH" => aggregateByEmailAndMonth
      case "EMAIL_DAY" => aggregateByEmailAndDay
      case "EMAIL_HOUR" => aggregateByEmailAndHour
      case unsupported =>
        throw new IllegalArgumentException(s"Unsupported aggregation '$aggregationName")
    }
  }

  object aggregateByEmail extends AggregationStrategy {
    override def aggregationKey(event: GerritJsonEvent): String = event.account.email
  }

  trait EmailAndTimeBasedAggregation extends AggregationStrategy {
    val dateFormat: DateFormat

    final override def aggregationKey(event: GerritJsonEvent): String = {
      s"${event.account.email}/${dateFormat.format(event.eventCreatedOn)}"
    }
  }

  object aggregateByEmailAndHour extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyyMMddHH")
  }

  object aggregateByEmailAndDay extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyyMMdd")

    override def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): DateTimeParts =
      super.decomposeTimeOfAggregatedEvent(event).copy(hour = 0)
  }

  object aggregateByEmailAndMonth extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyyMM")

    override def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): DateTimeParts =
      super.decomposeTimeOfAggregatedEvent(event).copy(day = 0, hour = 0)
  }

  object aggregateByEmailAndYear extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyy")

    override def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): DateTimeParts =
      super.decomposeTimeOfAggregatedEvent(event).copy(month = 0, day = 0, hour = 0)
  }
}