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

package com.gerritforge.analytics.support

import com.google.gerrit.extensions.client.{ChangeKind, ChangeStatus}
import com.google.gerrit.extensions.common._

import scala.collection.JavaConverters._

package DefaultValuesSupport {

  import java.sql.Timestamp

  import com.gerritforge.analytics.support.ops.AnalyticsTimeOps.CommonTimeOperations._

  object GerritModelDefaultValues {

    def defaultAccountInfo(
                            id: Int = 1000,
                            name: String = "Default AccountInfo User",
                            email: String = "default@email.com"
                          ): AccountInfo = {

      var accountInfo = new AccountInfo(id)

      accountInfo.name = name
      accountInfo.email = email

      accountInfo
    }

    def defaultGitPerson(
                          date: Timestamp = nowSqlTimestmap,
                          email: String = "default@email.com",
                          name: String = "Default Git User"
                        ): GitPerson = {

      var defaultGitPerson = new GitPerson()

      defaultGitPerson.date = date
      defaultGitPerson.email = email
      defaultGitPerson.name = name

      defaultGitPerson
    }

    def defaultApprovalInfo(
                             id: Int = 1000,
                             date: Timestamp = nowSqlTimestmap,
                             permittedVotingRange: VotingRangeInfo = new VotingRangeInfo(-2, 2),
                             email: String = "defaut@email.com",
                             name: String = "Default User",
                             username: String = "default-user",
                             value: Int = 2
                           ): ApprovalInfo = {

      var defaultApprovalInfo = new ApprovalInfo(id)

      defaultApprovalInfo.date = date
      defaultApprovalInfo.permittedVotingRange = permittedVotingRange
      defaultApprovalInfo.email = email
      defaultApprovalInfo.name = name
      defaultApprovalInfo.username = username
      defaultApprovalInfo.value = value

      defaultApprovalInfo
    }

    def defaultFileInfo(
                         binary: Boolean = false,
                         linesInserted: Int = 10,
                         linesDeleted: Int = 10,
                         oldPath: String = "default/oldpath.txt",
                         sizeDelta: Long = 1l,
                         size: Long = 1l
                       ): FileInfo = {

      var fileInfo = new FileInfo()

      fileInfo.binary = binary
      fileInfo.linesInserted = linesInserted
      fileInfo.linesDeleted = linesDeleted
      fileInfo.oldPath = oldPath
      fileInfo.sizeDelta = sizeDelta
      fileInfo.size = size

      fileInfo
    }


    def defaultRevisionInfo(
                             _number: Int = 1,
                             commit: CommitInfo = defaultCommitInfo(),
                             created: Timestamp = nowSqlTimestmap,
                             draft: Boolean = false,
                             kind: ChangeKind = ChangeKind.REWORK,
                             files: Map[String, FileInfo] = Map("default/filepath.txt" -> defaultFileInfo())
                           ): RevisionInfo = {


      var revision = new RevisionInfo()

      revision._number = 1
      revision.commit = defaultCommitInfo()
      revision.created = nowSqlTimestmap
      revision.description = "A default revision description"
      revision.draft = draft
      revision.kind = kind
      revision.files = files.asJava

      revision
    }


    def defaultCommitInfo(
                           commit: String = "CommitIdHashValue",
                           message: String = "A default commit message",
                           subject: String = "A default subject message",
                           author: GitPerson = defaultGitPerson(),
                           committer: GitPerson = defaultGitPerson()
                         ): CommitInfo = {

      var commitInfo = new CommitInfo()

      commitInfo.commit = commit
      commitInfo.message = message
      commitInfo.subject = subject
      commitInfo.author = author
      commitInfo.committer = committer

      commitInfo
    }

    def defaultCommentInfo(
                            author: AccountInfo = defaultAccountInfo(),
                            message: String = "Default comment message",
                            line: Int = 1,
                            path: String = "/default/filepath.scala",
                            patchSet: Int = 1,
                            id: String = "default-id",
                            unresolved: Boolean = true,
                            updated: Timestamp = nowSqlTimestmap
                          ): CommentInfo = {
      var defaultCommentInfo = new CommentInfo()

      defaultCommentInfo.author = author
      defaultCommentInfo.message = message
      defaultCommentInfo.line = line
      defaultCommentInfo.path = path
      defaultCommentInfo.patchSet = patchSet
      defaultCommentInfo.id = id
      defaultCommentInfo.unresolved = unresolved
      defaultCommentInfo.updated = updated

      defaultCommentInfo
    }

    def defaultLabelInfo(listApprovalInfo: List[ApprovalInfo] = List(defaultApprovalInfo())): LabelInfo = {
      var defaultLabelInfo = new LabelInfo

      defaultLabelInfo.all = listApprovalInfo.asJava

      defaultLabelInfo
    }

    def defaultChangeInfo(
                           id: String = "Project~ChangeIdHashValue",
                           _number: Int = 1,
                           changeId: String = "ChangeIdHashValue",
                           created: Timestamp = nowSqlTimestmap,
                           revisions: Map[String, RevisionInfo] = Map("defaultRevision" -> defaultRevisionInfo()),
                           owner: AccountInfo = defaultAccountInfo(),
                           insertions: Int = 10,
                           deletions: Int = 10,
                           project: String = "Default Project Name",
                           branch: String = "master",
                           status: ChangeStatus = ChangeStatus.NEW,
                           unresolvedCommentCount: Int = 1,
                           hasReviewStarted: Boolean = true,
                           labels: Map[String, LabelInfo] = Map("Code-Review" -> defaultLabelInfo())
                         ): ChangeInfo = {


      var changeInfo = new ChangeInfo()

      changeInfo.id = id
      changeInfo._number = _number
      changeInfo.changeId = changeId
      changeInfo.created = created
      changeInfo.revisions = revisions.asJava
      changeInfo.owner = owner
      changeInfo.insertions = insertions
      changeInfo.deletions = deletions
      changeInfo.project = project
      changeInfo.branch = branch
      changeInfo.status = status
      changeInfo.unresolvedCommentCount = unresolvedCommentCount
      changeInfo.hasReviewStarted = hasReviewStarted
      changeInfo.labels = labels.asJava

      changeInfo
    }
  }

}
