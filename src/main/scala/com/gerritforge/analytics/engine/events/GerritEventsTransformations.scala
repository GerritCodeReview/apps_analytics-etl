package com.gerritforge.analytics.engine.events

import java.text.{DateFormat, SimpleDateFormat}
import java.time.{LocalDateTime, ZoneOffset}

import com.gerritforge.analytics.engine.GerritAnalyticsTransformations.{CommitInfo, UserActivitySummary}
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}

object GerritEventsTransformations extends LazyLogging {

  def tryParseGerritTriggeredEvent(eventJson: String)(implicit eventParser: GerritJsonEventParser): Either[String, GerritJsonEvent] = {
    eventParser.fromJson(eventJson).fold[Either[String, GerritJsonEvent]](Left(eventJson))(Right(_))
  }

  implicit class PimpedJsonRDD(val self: RDD[String]) extends AnyVal {
    def parseEvents(implicit eventParser: GerritJsonEventParser): RDD[Either[String, GerritJsonEvent]] = {
      self.map(tryParseGerritTriggeredEvent)
    }
  }

  sealed trait AggregationStrategy {
    def aggregationKey(event: GerritJsonEvent): String

    def decomposeTimeOfAggregatedEvent(event: GerritJsonEvent): (Integer, Integer, Integer, Integer) = {
      val date = LocalDateTime.ofEpochSecond(event.eventCreatedOn, 0, ZoneOffset.UTC)
      (date.getYear, date.getMonthValue, date.getDayOfMonth, date.getHour)
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

  private def addCommitSummary(summaryTemplate: UserActivitySummary, changes: Iterable[ChangeMergedEvent])(implicit eventParser: GerritJsonEventParser): UserActivitySummary = {
    //      val branchesForAllCommits: Set[String] =
    //        changes.foldLeft(Set.empty[String]) { (accumulator, currentChange) =>
    //          commitIdToBranchesMap
    //            .get(currentChange.getNewRev)
    //            .fold(accumulator)(branchesForCurrentCommit => accumulator ++ branchesForCurrentCommit)
    //        }

    // We assume all the changes are consistent on the is_merge filter
    summaryTemplate.copy(
      num_commits = changes.size,
      commits = changes.map(changeMerged =>
        CommitInfo(changeMerged.newRev, changeMerged.eventCreatedOn, changeMerged.patchSet.parents.size > 1)
      ).toArray,
      last_commit_date = changes.map(_.eventCreatedOn).max,
      // We cannot calculate anything about files until we export the list of files in the gerrit event
      num_files = 0,
      num_distinct_files = 0,
      // We cannot calculate that until we change the parsing of the event to include those fields
      added_lines = changes.map(_.patchSet.sizeInsertions).sum,
      deleted_lines = changes.map(_.patchSet.sizeDeletions).sum
    )
  }

  /**
    * Extract up to two UserActivitySummary object from the given iterable of ChangeMerged events
    * depending if there is a mix of merge and non merge changes
    */
  def extractUserActivitySummary(changes: Iterable[ChangeMergedEvent],
                                 //commitIdToBranchesMap: Map[String, Set[String]],
                                 year: Integer,
                                 month: Integer,
                                 day: Integer,
                                 hour: Integer)(implicit eventParser: GerritJsonEventParser): Iterable[UserActivitySummary] = {

    changes.headOption.fold(List.empty[UserActivitySummary]) { firstChange =>
      val name = firstChange.account.name
      val email = firstChange.account.email

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

      val (mergeCommits, nonMergeCommits) =
        changes.foldLeft(List.empty[ChangeMergedEvent], List.empty[ChangeMergedEvent]) { case ((mergeCommits, nonMergeCommits), currentCommit) =>
          if(currentCommit.patchSet.parents.size > 1) {
            (currentCommit :: mergeCommits, nonMergeCommits)
          } else {
            (mergeCommits, currentCommit :: nonMergeCommits)
          }
        }

      List(
        (summaryTemplate.copy(is_merge = true), mergeCommits.reverse),
        (summaryTemplate.copy(is_merge = false), nonMergeCommits.reverse)
      )
        .filter(_._2.nonEmpty)
        .map { case (template, commits) =>
          addCommitSummary(template, commits)
        }
    }
  }

  implicit class PimpedGerritJsonEventRDD(val self: RDD[GerritJsonEvent]) extends AnyVal {
    /**
      * Returns the UTC date of the earliest event in the RDD
      * @return
      */
    def earliestEventTime : LocalDateTime = {
      val earliestEpoch = self.map { event =>
        event.eventCreatedOn
      }.min()

      LocalDateTime.ofEpochSecond(earliestEpoch, 0, ZoneOffset.UTC)
    }

//    def userActivitySummaryPerProject(aggregationStrategy: AggregationStrategy, branchesPerCommitByProject : RDD[(String, (String, Set[String]))])(implicit sparkContext: SparkContext): RDD[(String, UserActivitySummary)] = {
    def userActivitySummaryPerProject(aggregationStrategy: AggregationStrategy, eventParser: GerritJsonEventParser): RDD[(String, UserActivitySummary)] = {
      // Starting with a simple approach assuming the map of commits with branches per project is not too large
//      val broadcastBranchesPerCommitByProject = sparkContext.broadcast(
//          branchesPerCommitByProject
//            .aggregateByKey(Map.empty[String, Set[String]])(
//              seqOp = _ + _,
//              combOp = _ ++ _
//            ).collectAsMap()
//      )

      self
        .filter(_.isInstanceOf[ChangeMergedEvent])
        .asInstanceOf[RDD[ChangeMergedEvent]]
        .groupBy { event =>
          (event.change.project, aggregationStrategy.aggregationKey(event))
        }.flatMap { case ((project, _), changesPerUserAndTimeWindow) =>
          val (year, month, day, hour) = aggregationStrategy.decomposeTimeOfAggregatedEvent(changesPerUserAndTimeWindow.head)

//          val branchesPerCommitForThisProject = broadcastBranchesPerCommitByProject.value.getOrElse(project, Map.empty)

          extractUserActivitySummary(
              changesPerUserAndTimeWindow, year, month, day, hour
            )(eventParser).map(
              project -> _
            )
        }
    }

    // I think this one is going to miss probably many of the commit Ids in case of merges bringing many commits
    // into the main trunk
    def branchesPerCommitByProject : RDD[(String, (String, Set[String]))] =
      self
        .filter(_.isInstanceOf[RefUpdatedEvent])
        .asInstanceOf[RDD[RefUpdatedEvent]]
          .flatMap { event =>
            List(
              ((event.modifiedProject, event.refUpdate.oldRev), Set(event.refUpdate.refName)),
              ((event.modifiedProject, event.refUpdate.newRev), Set(event.refUpdate.refName))
            )
          }.reduceByKey(_ union _)
        .map { case ((project, commitId), branches) =>
          project -> (commitId -> branches)
        }
  }

  def convertAsDataFrame(self: RDD[(String, UserActivitySummary)])(implicit sqlContext: SQLContext): DataFrame = {
    import sqlContext.implicits._

    self.map { case (project, summary) =>
      (project, summary.name, summary.email, summary.year, summary.month, summary.day, summary.hour, summary.num_files, summary.num_distinct_files,
      summary.added_lines, summary.deleted_lines, summary.num_commits, summary.last_commit_date, summary.is_merge)
    }.toDF("project", "author", "email", "year", "month", "day", "hour", "num_files", "num_distinct_files",
    "added_lines", "deleted_lines", "num_commits", "last_commit_date", "is_merge")
  }

  implicit class PimpedUserActivitySummaryPerProjectRDD(val self: RDD[(String, UserActivitySummary)]) extends AnyVal {
    def asEtlDataFrame(implicit sqlContext: SQLContext) : DataFrame = {
      convertAsDataFrame(self)
    }
  }
}
