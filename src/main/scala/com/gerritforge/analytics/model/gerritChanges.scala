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


case class GerritAccountWithId(id: String, name: String, email: String, username: String)

trait GerritChangeJsonElement

case class ChangeHeader(
                         urlId: String,
                         changeId: String,
                         branch: String,
                         projectName: String,
                         subject: String,
                         status: String,
                         createdEpoch: Long,
                         lastUpdatedEpoch: Option[Long],
                         submittedEpoch: Option[Long],
                         submitterAccountId: Option[String],
                         insertions: Int,
                         deletions: Int,
                         unresolvedCommentCount: Int,
                         hasReviewStarted: Boolean,
                         changeNumber: Int,
                         changeOwnerAccountId: String
                       ) extends GerritChangeJsonElement


case class GerritComment(
                          id: String,
                          commentator: GerritAccountWithId,
                          unresolved: Boolean,
                          lineNumber: Int,
                          patchSetNumber: Int,
                          updateEpoch: Long,
                          message: String
                        ) extends GerritChangeJsonElement


case class UserVotingRange(min: Int, max: Int) extends GerritChangeJsonElement

case class CodeReviewScoreInfo(
                                accountId: String,
                                value: Int,
                                scoreEpoch: Long,
                                permittedVotingRage: UserVotingRange
                              ) extends GerritChangeJsonElement

case class Revision(
                     commitId: String,
                     kindOfRevision: String,
                     revisionNumber: Int,
                     revisionOwnerAccountId: Long,
                     revisionEpoch: Long,
                     revisionReference: String
                   ) extends GerritChangeJsonElement


case class FileCommentDetails(
                               fileName: String,
                               comments: List[GerritComment]
                             ) extends GerritChangeJsonElement


case class ChangeInformation(
                              changeHeader: ChangeHeader,
                              revisions: List[Revision],
                              scores: List[CodeReviewScoreInfo],
                              filesCommented: List[FileCommentDetails]
                            ) extends GerritChangeJsonElement


sealed trait GerritChangeStatus {
  def stringValue: String
}

case object Open extends GerritChangeStatus {
  val stringValue: String = "OPEN"
}

case object Merged extends GerritChangeStatus {
  val stringValue: String = "MERGED"
}

case object Abandoned extends GerritChangeStatus {
  val stringValue: String = "ABANDONED"
}

case object Draft extends GerritChangeStatus {
  val stringValue: String = "DRAFT"
}
