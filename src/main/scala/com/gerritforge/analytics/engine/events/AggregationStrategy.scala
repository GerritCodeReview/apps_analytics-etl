package com.gerritforge.analytics.engine.events

import java.text.{DateFormat, SimpleDateFormat}
import java.time.{LocalDateTime, ZoneOffset}

sealed trait AggregationStrategy extends Serializable {
  def aggregationKey(event: GerritJsonEvent): String

  def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): (Integer, Integer, Integer, Integer) = {
    val date = LocalDateTime.ofEpochSecond(event.eventCreatedOn, 0, ZoneOffset.UTC)
    (date.getYear, date.getMonthValue, date.getDayOfMonth, date.getHour)
  }
}

object AggregationStrategy {

  @throws(classOf[IllegalArgumentException])
  def byName(aggregationName: String): AggregationStrategy = {
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

    override def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): (Integer, Integer, Integer, Integer) =
      super.decomposeTimeOfAggregatedEvent(event).copy(_4 = 0)
  }

  object aggregateByEmailAndMonth extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyyMM")

    override def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): (Integer, Integer, Integer, Integer) =
      super.decomposeTimeOfAggregatedEvent(event).copy(_3 = 0, _4 = 0)
  }

  object aggregateByEmailAndYear extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyy")

    override def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): (Integer, Integer, Integer, Integer) =
      super.decomposeTimeOfAggregatedEvent(event).copy(_2 = 0, _3 = 0, _4 = 0)
  }
}