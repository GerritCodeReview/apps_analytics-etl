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

package com.gerritforge.analytics.model

import java.util

import com.google.gerrit.extensions.client.ChangeStatus
import com.google.gerrit.extensions.common.{ApprovalInfo, ChangeInfo, CommentInfo}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try

sealed trait ChangesReport

case class GenericTopChangesReport(
                                    change_id: String,
                                    change_subject: String,
                                    change_owner: String,
                                    change_status: String,
                                    change_create_ts: Long,
                                    change_last_update_ts: Option[Long],
                                    submitted_ts: Option[Long],
                                    number_of_patchsets: Int,
                                    number_of_commits: Int,
                                    number_of_comments: Int,
                                    number_of_lines_inserted: Int,
                                    number_of_lines_deleted: Int,
                                    number_of_files_modidifed: Int,
                                    number_of_unresolved_comments: Int,
                                    last_commit_ts: Long
                                  ) extends ChangesReport

case class TopRiskyChangesReport(
                                  change_id: String,
                                  change_owner_id: Int,
                                  change_status: String,
                                  number_of_lines_inserted: Int,
                                  number_of_lines_deleted: Int,
                                  number_of_files_modidifed: Int,
                                  total_num_lines_modified: Int,
                                  number_of_comments: Int,
                                  risk_rank: Int
                                ) extends ChangesReport


case class ChangesWithModificationStats(
                                  change_id: String,
                                  change_owner_id: Int,
                                  change_status: String,
                                  number_of_lines_inserted: Int,
                                  number_of_lines_deleted: Int,
                                  number_of_files_modidifed: Int,
                                  total_num_lines_modified: Int,
                                  number_of_comments: Int
                                )

case class GerritAccountWithId(id: Int, name: String, email: String, username: String)


case class GerritComment(
                          id: String,
                          commentator: GerritAccountWithId,
                          unresolved: Boolean,
                          lineNumber: Int,
                          patchSetNumber: Int,
                          updateEpoch: Long,
                          message: String
                        )

object GerritComment {

  def convertFromFileComments(comments: List[CommentInfo]): List[GerritComment] = {
    comments.map { comment =>
      GerritComment(
        comment.id,
        GerritAccountWithId(comment.author._accountId.toInt, comment.author.name, comment.author.email, comment.author.username),
        comment.unresolved.booleanValue(),
        comment.line.toInt,
        comment.patchSet.toInt,
        comment.updated.getTime,
        comment.message
      )
    }
  }
}


case class UserVotingRange(min: Int, max: Int)

case class CodeReviewInfo(
                           accountId: Int,
                           value: Int,
                           scoreEpoch: Long,
                           permittedVotingRage: UserVotingRange
                         )

object CodeReviewInfo {
  def fromGerritChangeInfo(changeInfo: ChangeInfo) = {
    changeInfo.labels.get("Code-Review").all.asScala.map { approvalInfo: ApprovalInfo =>
      CodeReviewInfo(
        approvalInfo._accountId.toInt,
        approvalInfo.value.toInt,
        approvalInfo.date.getTime,
        UserVotingRange(approvalInfo.permittedVotingRange.min, approvalInfo.permittedVotingRange.max)
      )
    }
  }.toList
}

case class FileInformation(
                            currentPath: String,
                            isBinary: Boolean,
                            oldPath: Option[String],
                            linesInserted: Int,
                            linesDeleted: Int,
                            sizeDelta: Long,
                            size: Long
                          )

case class Revision(
                     commitId: String,
                     kindOfRevision: String,
                     revisionNumber: Int,
                     revisionOwnerAccountId: String,
                     revisionEpoch: Long,
                     isCurrentRevision: Boolean,
                     revisionReference: String,
                     filesCommitted: List[FileInformation]
                   )

object Revision {
  def fromGerritChangeInfo(changeInfo: ChangeInfo): List[Revision] = {
    changeInfo.revisions.asScala.map { case (key, revisionInfo) =>

      val filesCommited: List[FileInformation] =
        revisionInfo.files.asScala.map { case (currentFilePath, fileInfo) =>
          val isBinary = if (Option(fileInfo.binary).isDefined) fileInfo.binary.booleanValue() else false
          val maybeOldPath = Option(fileInfo.oldPath)
          val deleted = if(Option(fileInfo.linesDeleted).isDefined) fileInfo.linesDeleted.toInt else 0
          val inserted = if(Option(fileInfo.linesInserted).isDefined) fileInfo.linesInserted.toInt else 0
          val sizeDelta: Long = if(Option(fileInfo.sizeDelta).isDefined) fileInfo.sizeDelta.longValue() else 0
          val fileSize: Long = if(Option(fileInfo.size).isDefined) fileInfo.size.longValue() else 0

          FileInformation(currentFilePath, isBinary, maybeOldPath, inserted, deleted, sizeDelta, fileSize)
        }.toList

      Revision(
        key,
        revisionInfo.kind.toString,
        revisionInfo._number,
        revisionInfo.commit.author.name,
        revisionInfo.created.getTime,
        revisionInfo.isCurrent,
        revisionInfo.ref,
        filesCommited
      )
    }.toList
  }
}


case class FileCommentDetails(
                               fileName: String,
                               comments: List[GerritComment]
                             )

object FileCommentDetails {
  def fromFileComments(fileName: String, comments: List[CommentInfo]): FileCommentDetails = {
    FileCommentDetails(
      fileName,
      GerritComment.convertFromFileComments(comments)
    )
  }
}


case class ChangeInformation(
                              id: String,
                              changeId: String,
                              branch: String,
                              projectName: String,
                              subject: String,
                              status: GerritChangeStatus,
                              createdEpoch: Long,
                              lastUpdatedEpoch: Option[Long],
                              submittedEpoch: Option[Long],
                              insertions: Int,
                              deletions: Int,
                              unresolvedCommentCount: Int,
                              hasReviewStarted: Boolean,
                              changeNumber: Int,
                              changeOwnerAccountId: Int,
                              revisions: List[Revision],
                              codeReviews: List[CodeReviewInfo],
                              filesCommented: List[FileCommentDetails]
                            )

object ChangeInformation {

  /**
    * Merges ChangeInfo and CommentInfo if available and converts to internal model ChangeInformation
    *
    * @param changeInfo - ChangeInfo from gerrit API
    * @param comments   - CommentInfo from gerrit API
    * @return - Returns ChangeInformation
    */
  def convertFromGerritChangeInfo(
                                   changeInfo: ChangeInfo,
                                   comments: mutable.Map[String, util.List[CommentInfo]] = mutable.Map.empty[String, util.List[CommentInfo]]
                                 ): ChangeInformation = {

    val changeInformationWithoutComments = convertFromGerritChangeInfoToChangeInformation(changeInfo)

    comments.isEmpty match {
      case true => changeInformationWithoutComments
      case false =>
        val filesComments: List[FileCommentDetails] = comments.map { case (filePath, comments) =>
          FileCommentDetails.fromFileComments(filePath, comments.asScala.toList)
        }.toList

        changeInformationWithoutComments.copy(filesCommented = filesComments)
    }
  }

  private def convertFromGerritChangeInfoToChangeInformation(changeInfo: ChangeInfo): ChangeInformation = {

    val revisions = Revision.fromGerritChangeInfo(changeInfo)
    val codeReview = CodeReviewInfo.fromGerritChangeInfo(changeInfo)

    ChangeInformation(
      changeInfo.id,
      changeInfo.changeId,
      changeInfo.branch,
      changeInfo.project,
      changeInfo.subject,
      ChangeStatusOps.fromEnumChangeStatusToChangeStatus(changeInfo.status),
      changeInfo.created.getTime,
      Try(changeInfo.updated.getTime).toOption,
      Try(changeInfo.submitted.getTime).toOption,
      changeInfo.insertions.intValue(),
      changeInfo.deletions.intValue(),
      changeInfo.unresolvedCommentCount.intValue(),
      changeInfo.hasReviewStarted.booleanValue(),
      changeInfo._number,
      changeInfo.owner._accountId.toInt,
      revisions,
      codeReview,
      List.empty[FileCommentDetails]
    )
  }
}


sealed trait GerritChangeStatus extends Serializable {
  def stringValue: String
}

case object New extends GerritChangeStatus {
  override val stringValue: String = "OPEN"
}

case object Merged extends GerritChangeStatus {
  override val stringValue: String = "MERGED"
}

case object Abandoned extends GerritChangeStatus {
  override val stringValue: String = "ABANDONED"
}

case object Draft extends GerritChangeStatus {
  override val stringValue: String = "DRAFT"
}

case object Submitted extends GerritChangeStatus {
  override val stringValue: String = "SUBMITTED"
}


object ChangeStatusOps {

  def fromEnumChangeStatusToChangeStatus(changeStatus: ChangeStatus): GerritChangeStatus = {
    changeStatus match {
      case ChangeStatus.NEW => New
      case ChangeStatus.MERGED => Merged
      case ChangeStatus.ABANDONED => Abandoned
      case ChangeStatus.DRAFT => Draft
      case ChangeStatus.SUBMITTED => Submitted
    }
  }

  def fromStringToChangeStatus(strChangeStatus: String): GerritChangeStatus = {
    val upperCase = strChangeStatus.toUpperCase()

    upperCase match {
      case New.stringValue => New
      case Merged.stringValue => Merged
      case Abandoned.stringValue => Abandoned
      case Draft.stringValue => Draft
    }
  }
}
