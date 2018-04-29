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

package com.gerritforge.analytics.engine

import java.util

import com.gerritforge.analytics.model._
import com.google.gerrit.extensions.common.{ChangeInfo, CommentInfo}
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}

import scala.collection.mutable
import scala.util.Try

object ChangesAnalyticsProcessor {

  def convertToAnalyticsDataModel(changeInfoAndMaybeComments: List[(ChangeInfo, mutable.Map[String, util.List[CommentInfo]])])(sparkSession: SparkSession): Dataset[ChangeInformation] = {

    implicit val enconder = Encoders.kryo[ChangeInformation]

    import sparkSession.implicits._
    changeInfoAndMaybeComments.map { case (changeInfo, comments) =>
      ChangeInformation.convertFromGerritChangeInfo(changeInfo, comments)
    }.toDS()
  }

  def convertToAnalyticsDataModelWithoutComments(changeInfoAndMaybeComments: List[ChangeInfo])(sparkSession: SparkSession): Dataset[ChangeInformation] = {
    implicit val enconder = Encoders.kryo[ChangeInformation]

    import sparkSession.implicits._
    changeInfoAndMaybeComments.map { case (changeInfo) =>
      ChangeInformation.convertFromGerritChangeInfo(changeInfo)
    }.toDS()
  }

  def calculateNumberOfFilesModifiedInLastRevision(change: ChangeInformation): Int = getChangeLatestRevision(change).filesCommitted.size

  def calculateNumberOfComments(change: ChangeInformation): Int =
    change
      .filesCommented
      .map(_.comments.size).fold(0)(_ + _)


  def getChangeLatestRevision(change: ChangeInformation): Revision = change.revisions.sortBy(-_.revisionEpoch).head

  def calculateMaxPatchSetNumberForChange(change: ChangeInformation): Int = {

    val patchesList: List[Int] = change
      .filesCommented
      .flatMap(_.comments.map(_.patchSetNumber))

    calculateMaxFromList(patchesList)
  }


  def calculateMaxFromList(list: List[Int]) =
    Try(list.sorted(Ordering.Int.reverse).head).getOrElse(0)

}

object ChangesReportGenerator {

  object implicits {

    implicit class ChangeInformationDatasetTransformations(val changes: Dataset[ChangeInformation]) {

      import changes.sparkSession.implicits._


      def generateRiskyChangesReport: Dataset[TopRiskyChangesReport] = {
        val changeStats = changes
          .filter(change => List(New, Draft, Submitted).contains(change.status))
          .map { change =>
            import ChangesAnalyticsProcessor._

            val numberOfFilesModified: Int = calculateNumberOfFilesModifiedInLastRevision(change)
            val totalNumLinesModifications: Int = change.insertions + change.deletions
            val numberOfComments = calculateNumberOfComments(change)

            ChangesWithModificationStats(
              change.changeId,
              change.changeOwnerAccountId,
              change.status.stringValue,
              change.insertions,
              change.deletions,
              numberOfFilesModified,
              totalNumLinesModifications,
              numberOfComments
            )
          }

        rankChangeModificationStats(changeStats)
      }

      def generateGenericTopChangesReport: Dataset[GenericTopChangesReport] = {
        changes.map { change =>
          import ChangesAnalyticsProcessor._

          val numberOfComments = calculateNumberOfComments(change)
          val lastCommit = getChangeLatestRevision(change)
          val numberOfFilesModified = calculateNumberOfFilesModifiedInLastRevision(change)
          val maxNumberOfPatchSets = calculateMaxPatchSetNumberForChange(change)

          GenericTopChangesReport(
            change.changeId,
            change.subject,
            change.changeOwnerAccountId.toString,
            change.status.stringValue,
            change.createdEpoch,
            change.lastUpdatedEpoch,
            change.submittedEpoch,
            maxNumberOfPatchSets,
            change.revisions.size,
            numberOfComments,
            change.insertions,
            change.deletions,
            numberOfFilesModified,
            change.unresolvedCommentCount,
            lastCommit.revisionEpoch
          )
        }
      }
    }

  }

  def rankChangeModificationStats(changeStats: Dataset[ChangesWithModificationStats]): Dataset[TopRiskyChangesReport] = {
    import changeStats.sparkSession.implicits._
    import org.apache.spark.sql.expressions.Window


    val orderingWithNoBucketPartition = Window.orderBy('number_of_comments asc, 'total_num_lines_modified desc, 'number_of_files_modidifed desc)

    import org.apache.spark.sql.functions._

    changeStats
      .withColumn("risk_rank", rank over orderingWithNoBucketPartition)
      .as[TopRiskyChangesReport]
  }

}