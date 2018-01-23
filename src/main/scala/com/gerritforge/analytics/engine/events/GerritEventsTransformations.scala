package com.gerritforge.analytics.engine.events

import java.text.{DateFormat, SimpleDateFormat}
import java.util.Calendar.YEAR
import java.util.{Calendar, TimeZone}

import com.gerritforge.analytics.engine.GerritAnalyticsTransformations.{CommitInfo, UserActivitySummary}
import com.gerritforge.analytics.engine.events.GerritEventsTransformations.SerializableGerritEvent
import com.sonymobile.tools.gerrit.gerritevents.GerritJsonEventFactory
import com.sonymobile.tools.gerrit.gerritevents.dto.{GerritEventType, GerritJsonEvent}
import com.sonymobile.tools.gerrit.gerritevents.dto.events.{ChangeMerged, GerritTriggeredEvent, RefUpdated}
import com.typesafe.scalalogging.LazyLogging
import net.sf.json.JSONObject
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object GerritEventsTransformations extends LazyLogging {

  /**
    * This class is necessary because the GerritJsonEvent hierarchy is not serializable
    * We need then to move around its pre-serialized form, in our case the JSON we used to get the event in the first place
    * The event is generated and cached on demand on the consumer side
    *
    * @param json     The JSON serialized form of the event
    * @param classTag The concrete type of the event
    * @tparam T
    */
  class SerializableGerritEvent[+T <: GerritJsonEvent](protected[events] val json: String)(implicit classTag: ClassTag[T]) extends Serializable {
    /**
      * Generates the event from the JSON representation.
      * Mutating the event (the class is not immutable) is not going to affect the content of this object
      */
    def asEvent(): T = event

    @transient
    private lazy val event: T = {
      val result: T = classTag.runtimeClass.newInstance().asInstanceOf[T]
      result.fromJson(JSONObject.fromObject(json))
      result
    }

    def canEqual(other: Any): Boolean = other.isInstanceOf[SerializableGerritEvent[T]]

    override def equals(other: Any): Boolean = other match {
      case that: SerializableGerritEvent[T] =>
        (that canEqual this) &&
          event == that.event

      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(event)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
  }

  object SerializableGerritEvent {
    def apply[T <: GerritJsonEvent](json: String)(implicit classTag: ClassTag[T]): SerializableGerritEvent[T] =
      new SerializableGerritEvent[T](json)
  }

  def tryParseGerritTriggeredEvent(eventJson: String): Either[String, SerializableGerritEvent[GerritJsonEvent]] = {
    Option(GerritJsonEventFactory.getEventIfInteresting(eventJson)).fold[Either[String, SerializableGerritEvent[GerritJsonEvent]]](Left(eventJson)) { parsedEvent: GerritJsonEvent =>
      Right(SerializableGerritEvent(eventJson)(ClassTag[GerritJsonEvent](parsedEvent.getClass)))
    }
  }

  implicit class PimpedJsonRDD(val self: RDD[String]) extends AnyVal {
    def parseEvents: RDD[Either[String, SerializableGerritEvent[GerritJsonEvent]]] = {
      self.map(tryParseGerritTriggeredEvent)
    }
  }

  sealed trait AggregationStrategy {
    def aggregationKey(event: GerritTriggeredEvent): String

    def decomposeTimeOfAggregatedEvent(event: GerritTriggeredEvent): (Integer, Integer, Integer, Integer) = {
      val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
      calendar.setTime(event.getEventCreatedOn)
      (calendar.get(YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), calendar.get(Calendar.HOUR))
    }
  }

  object aggregateByEmail extends AggregationStrategy {
    override def aggregationKey(event: GerritTriggeredEvent): String = event.getAccount.getEmail
  }

  trait EmailAndTimeBasedAggregation extends AggregationStrategy {
    val dateFormat: DateFormat

    final override def aggregationKey(event: GerritTriggeredEvent): String = {
      s"${event.getAccount.getEmail}/${dateFormat.format(event.getEventCreatedOn)}"
    }
  }

  object aggregateByEmailAndHour extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyyMMddHH")
  }

  object aggregateByEmailAndDay extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyyMMdd")

    override def decomposeTimeOfAggregatedEvent(event: GerritTriggeredEvent): (Integer, Integer, Integer, Integer) =
      super.decomposeTimeOfAggregatedEvent(event).copy(_4 = 0)
  }

  object aggregateByEmailAndMonth extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyyMM")

    override def decomposeTimeOfAggregatedEvent(event: GerritTriggeredEvent): (Integer, Integer, Integer, Integer) =
      super.decomposeTimeOfAggregatedEvent(event).copy(_3 = 0, _4 = 0)
  }

  object aggregateByEmailAndYear extends EmailAndTimeBasedAggregation {
    val dateFormat = new SimpleDateFormat("yyyy")

    override def decomposeTimeOfAggregatedEvent(event: GerritTriggeredEvent): (Integer, Integer, Integer, Integer) =
      super.decomposeTimeOfAggregatedEvent(event).copy(_2 = 0, _3 = 0, _4 = 0)
  }

  /**
    * Extract up to two UserActivitySummary object from the given iterable of ChangeMerged events
    * depending if there is a mix of merge and non merge changes
    *
    * @param changesByUser
    * @return
    */
  def extractUserActivitySummary(changesByUser: Iterable[ChangeMerged],
                                 commitIdToBranchesMap: Map[String, Set[String]],
                                 year: Integer,
                                 month: Integer,
                                 day: Integer,
                                 hour: Integer): Iterable[UserActivitySummary] = {
    def addCommitSummary(summaryTemplate: UserActivitySummary, changes: Iterable[ChangeMerged]): UserActivitySummary = {
      val branchesForAllCommits: Set[String] =
        changes.foldLeft(Set.empty[String]) { (accumulator, currentChange) =>
          commitIdToBranchesMap
            .get(currentChange.getNewRev)
            .fold(accumulator)(branchesForCurrentCommit => accumulator ++ branchesForCurrentCommit)
        }

      // We assume all the changes are consistent on the is_merge filter
      summaryTemplate.copy(
        num_commits = changes.size,
        commits = changes.map(changeMerged =>
          CommitInfo(changeMerged.getNewRev, changeMerged.getEventCreatedOn.getTime, changeMerged.getPatchSet.getParents.size() > 1)
        ).toArray,

        // We cannot calculate anything about files until we export the list of files in the gerrit event
        num_files = 0,
        num_distinct_files = 0,

        // We cannot calculate that until we change the parsing of the event to include those fields
        added_lines = 0,
        deleted_lines = 0

      )
    }

    changesByUser.headOption.fold(List.empty[UserActivitySummary]) { firstChange =>
      val name = firstChange.getAccount.getName
      val email = firstChange.getAccount.getEmail

      val summaryTemplate = UserActivitySummary(
        year = year,
        month = month,
        day = day,
        hour = hour,
        name = name,
        email = email,
        num_commits = 0,
        num_files = 0,
        num_distinct_files = 0,
        added_lines = 0,
        deleted_lines = 0,
        commits = Array.empty,
        last_commit_date = 0l,
        is_merge = false)

      List(
        addCommitSummary(summaryTemplate.copy(is_merge = false), changesByUser.filter(_.getPatchSet.getParents.size() <= 1))
      )
    }
  }

  implicit class PimpedSerializableGerritEventRDD(val self: RDD[SerializableGerritEvent[GerritJsonEvent]]) extends AnyVal {
    def userActivitySummaryPerProject(aggregationStrategy: AggregationStrategy, branchesPerCommitByProject : RDD[(String, (String, Set[String]))])(implicit sparkContext: SparkContext): RDD[(String, UserActivitySummary)] = {
      // Starting with a simple approach assuming the map of commits with branches per project is not too large
      val broadcastBranchesPerCommitByProject = sparkContext.broadcast(
          branchesPerCommitByProject
            .aggregateByKey(Map.empty[String, Set[String]])(
              seqOp = _ + _,
              combOp = _ ++ _
            ).collectAsMap()
      )

      self
        .filter(_.asEvent().isInstanceOf[ChangeMerged])
        .asInstanceOf[RDD[SerializableGerritEvent[ChangeMerged]]]
        .groupBy { event =>
          (event.asEvent().getModifiedProject, aggregationStrategy.aggregationKey(event.asEvent()))
        }.flatMap { case ((project, _), changesPerUserAndTimeWindow) =>
          val (year, month, day, hour) = aggregationStrategy.decomposeTimeOfAggregatedEvent(changesPerUserAndTimeWindow.head.asEvent())

          val branchesPerCommitForThisProject = broadcastBranchesPerCommitByProject.value.getOrElse(project, Map.empty)

          extractUserActivitySummary(
              changesPerUserAndTimeWindow.map(_.asEvent()), branchesPerCommitForThisProject, year, month, day, hour
            ).map(
              project -> _
            )
        }
    }

    // I think this one is going to miss probably many of the commit Ids in case of merges bringing many commits
    // into the main trunk
    def branchesPerCommitByProject : RDD[(String, (String, Set[String]))] =
      self
        .filter(_.asEvent().isInstanceOf[RefUpdated])
        .asInstanceOf[RDD[SerializableGerritEvent[RefUpdated]]]
          .flatMap { serializableEvent =>
            val event = serializableEvent.asEvent()
            List(
              ((event.getModifiedProject, event.getRefUpdate.getOldRev), Set(event.getRefUpdate.getRefName)),
              ((event.getModifiedProject, event.getRefUpdate.getNewRev), Set(event.getRefUpdate.getRefName))
            )
          }.reduceByKey(_ union _)
        .map { case ((project, commitId), branches) =>
          project -> (commitId -> branches)
        }
  }

}
