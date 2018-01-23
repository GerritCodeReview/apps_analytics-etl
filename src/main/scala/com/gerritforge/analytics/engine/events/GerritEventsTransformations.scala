package com.gerritforge.analytics.engine.events

import java.time.{LocalDateTime, ZoneOffset}

import com.gerritforge.analytics.engine.GerritAnalyticsTransformations.{CommitInfo, UserActivitySummary}
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext, SparkSession}


object GerritEventsTransformations extends LazyLogging {

  implicit class PimpedJsonRDD(val self: RDD[String]) extends AnyVal {
    def parseEvents(implicit eventParser: GerritJsonEventParser): RDD[Either[String, GerritJsonEvent]] = {
      self.map(tryParseGerritTriggeredEvent)
    }
  }

  implicit class PimpedGerritJsonEventRDD(val self: RDD[GerritJsonEvent]) extends AnyVal {
    /**
      * Returns the UTC date of the earliest event in the RDD
      *
      * @return
      */
    def earliestEventTime: LocalDateTime = {
      val earliestEpoch = self.map { event =>
        event.eventCreatedOn
      }.min()

      LocalDateTime.ofEpochSecond(earliestEpoch, 0, ZoneOffset.UTC)
    }

    def repositoryWithNewRevisionEvents: RDD[GerritRefHasNewRevisionEvent] = self.collect { case e: GerritRefHasNewRevisionEvent => e }
  }

  implicit class PimpedRepositoryModifiedGerritEventRDD(val self: RDD[GerritRefHasNewRevisionEvent]) extends AnyVal {
    def removeEventsForCommits(commitsToExclude: RDD[String]): RDD[GerritRefHasNewRevisionEvent] = {
      self
        .keyBy(_.newRev)
        .leftOuterJoin(commitsToExclude.keyBy(identity))
        .flatMap[GerritRefHasNewRevisionEvent] {
        case (_, (_, Some(_))) => None

        case (_, (event, None)) => Some(event)
      }
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
        .collect { case e: ChangeMergedEvent => e }
        .groupBy { event =>
          (event.change.project, aggregationStrategy.aggregationKey(event))
        }.flatMap { case ((project, _), changesPerUserAndTimeWindow) =>
        val (year, month, day, hour) = aggregationStrategy.decomposeTimeOfAggregatedEvent(changesPerUserAndTimeWindow.head)

        //          val branchesPerCommitForThisProject = broadcastBranchesPerCommitByProject.value.getOrElse(project, Map.empty)

        extractUserActivitySummary(
          changesPerUserAndTimeWindow, year, month, day, hour
        ).map(
          project -> _
        )
      }
    }

    // I think this one is going to miss probably many of the commit Ids in case of merges bringing many commits
    // into the main trunk
    def branchesPerCommitByProject: RDD[(String, (String, Set[String]))] =
      self
        .collect { case e: RefUpdatedEvent => e }
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

  implicit class PimpedUserActivitySummaryPerProjectRDD(val self: RDD[(String, UserActivitySummary)]) extends AnyVal {
    def asEtlDataFrame(implicit sqlContext: SQLContext): DataFrame = {
      convertAsDataFrame(self)
    }
  }

  def tryParseGerritTriggeredEvent(eventJson: String)(implicit eventParser: GerritJsonEventParser): Either[String, GerritJsonEvent] = {
    eventParser.fromJson(eventJson).fold[Either[String, GerritJsonEvent]](Left(eventJson))(Right(_))
  }


  private def addCommitSummary(summaryTemplate: UserActivitySummary, changes: Iterable[ChangeMergedEvent]): UserActivitySummary = {
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
      added_lines = changes.map(_.patchSet.sizeInsertions).sum,
      deleted_lines = changes.map(_.patchSet.sizeDeletions).sum,
      // We cannot calculate anything about files until we export the list of files in the gerrit event
      num_files = 0,
      num_distinct_files = 0
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
                                 hour: Integer): Iterable[UserActivitySummary] = {

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
          if (currentCommit.patchSet.parents.size > 1) {
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

  def convertAsDataFrame(self: RDD[(String, UserActivitySummary)])(implicit sqlContext: SQLContext): DataFrame = {
    import sqlContext.implicits._

    self.map { case (project, summary) =>
      (project, summary.name, summary.email, summary.year, summary.month, summary.day, summary.hour, summary.num_files, summary.num_distinct_files,
        summary.added_lines, summary.deleted_lines, summary.num_commits, summary.last_commit_date, summary.is_merge, summary.commits)
    }.toDF("project", "author", "email", "year", "month", "day", "hour", "num_files", "num_distinct_files",
      "added_lines", "deleted_lines", "num_commits", "last_commit_date", "is_merge", "commits")
  }

  def getContributorStatsFromGerritEvents(events: RDD[GerritRefHasNewRevisionEvent],
                                          commitsToExclude: RDD[String],
                                          aggregationStrategy: AggregationStrategy)(implicit spark: SparkSession) = {
    implicit val sqlCtx = spark.sqlContext

    events
      .removeEventsForCommits(commitsToExclude)
      .userActivitySummaryPerProject(aggregationStrategy, EventParser)
      .asEtlDataFrame
  }

}
