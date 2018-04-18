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


package com.gerritforge.analytics.extractors

import com.gerritforge.analytics.common.ops.AnalyticsTimeOps.AnalyticsDateTimeFormater
import com.gerritforge.analytics.model._
import org.json4s
import org.json4s.{DefaultFormats, JValue}

import scala.util.Try

trait JsonDataExtractor[T <: GerritChangeJsonElement] {
  implicit val jsonFormats = DefaultFormats

  def extractSingleElement(jsonContent: json4s.JValue): T

  def extractListOfElements(jsonContent: json4s.JValue): List[T]
}


object ChangeHeaderExtractor extends JsonDataExtractor[ChangeHeader] {

  override def extractListOfElements(jsonContent: JValue): List[ChangeHeader] = {
    jsonContent.extract[Array[JValue]].map { changeWithHeader =>
      extractSingleElement(changeWithHeader)
    }.toList
  }

  override def extractSingleElement(jsonContent: json4s.JValue): ChangeHeader = {
    import com.gerritforge.analytics.common.ops.AnalyticsTimeOps.implicits._

    val urlId: String = (jsonContent \ "id").extract[String]
    val changeId: String = (jsonContent \ "change_id").extract[String]
    val projectName: String = (jsonContent \ "project").extract[String]
    val subject: String = (jsonContent \ "subject").extract[String]
    val branchName: String = (jsonContent \ "branch").extract[String]
    val status: String = (jsonContent \ "status").extract[String]
    val createdEpoch: Long = (jsonContent \ "created").extract[String]
      .parseStringToUTCSecondsEpoch(AnalyticsDateTimeFormater.yyyy_MM_dd_HHmmss_SSSSSSSSS)
    val updatedEpoch: Option[Long] = Try((jsonContent \ "updated").extract[String]
      .parseStringToUTCSecondsEpoch(AnalyticsDateTimeFormater.yyyy_MM_dd_HHmmss_SSSSSSSSS)).toOption
    val submittedEpoch: Option[Long] = Try((jsonContent \ "submitted").extract[String]
      .parseStringToUTCSecondsEpoch(AnalyticsDateTimeFormater.yyyy_MM_dd_HHmmss_SSSSSSSSS)).toOption
    val submitterAccountId: Option[String] = Try((jsonContent \ "submitter" \ "_account_id").extract[String]).toOption
    val numInsertions: Int = (jsonContent \ "insertions").extract[Int]
    val numDeletions: Int = (jsonContent \ "deletions").extract[Int]
    val unresolvedCommentsCount: Int = (jsonContent \ "unresolved_comment_count").extract[Int]
    val hasReviewStarted = (jsonContent \ "has_review_started").extract[Boolean]
    val changeNumber: Int = (jsonContent \ "_number").extract[Int]
    val ownerAccountId: String = (jsonContent \ "owner" \ "_account_id").extract[String]

    ChangeHeader(
      urlId,
      changeId,
      branchName,
      projectName,
      subject,
      status,
      createdEpoch,
      updatedEpoch,
      submittedEpoch,
      submitterAccountId,
      numInsertions,
      numDeletions,
      unresolvedCommentsCount,
      hasReviewStarted,
      changeNumber,
      ownerAccountId
    )
  }
}


object ChangeCodeReviewsExtractor extends JsonDataExtractor[CodeReviewScoreInfo] {

  override def extractListOfElements(jsonContent: JValue): List[CodeReviewScoreInfo] = {
    (jsonContent \ "labels" \ "Code-Review" \ "all").extract[List[JValue]].map { review =>
      extractSingleElement(review)
    }
  }

  def extractSingleElement(review: json4s.JValue): CodeReviewScoreInfo = {
    import com.gerritforge.analytics.common.ops.AnalyticsTimeOps.implicits._


    val accountId = (review \ "_account_id").extract[String]
    val value = (review \ "value").extract[Int]
    val reviewDateString = (review \ "date").extract[String]
    val reviewDate: Long = reviewDateString
      .parseStringToUTCSecondsEpoch(AnalyticsDateTimeFormater.yyyy_MM_dd_HHmmss_SSSSSSSSS)

    val permittedVotingRangeMax = (review \ "permitted_voting_range" \ "max").extract[Int]
    val permittedVotingRangeMin = (review \ "permitted_voting_range" \ "min").extract[Int]
    val permittedRange = UserVotingRange(permittedVotingRangeMin, permittedVotingRangeMax)

    CodeReviewScoreInfo(accountId, value, reviewDate, permittedRange)
  }
}

object ChangeRevisionsExtractor extends JsonDataExtractor[Revision] {

  override def extractListOfElements(jsonContent: JValue): List[Revision] = {
    val revisionsJValue: json4s.JValue = (jsonContent \ "revisions")
    val revisionCommitIds: Iterable[String] = revisionsJValue.values.asInstanceOf[Map[String, String]].keys

    revisionCommitIds.map { commitId =>
      val revisionDetails: json4s.JValue = (revisionsJValue \ commitId)
      extractSingleElement(revisionDetails).copy(commitId = commitId)
    }.toList
  }

  override def extractSingleElement(revisionDetails: json4s.JValue): Revision = {
    import com.gerritforge.analytics.common.ops.AnalyticsTimeOps.implicits._

    val kindOfRevision = (revisionDetails \ "kind").extract[String]
    val revisionNumber = (revisionDetails \ "_number").extract[Int]
    val revisionCreatedDateStr: String = (revisionDetails \ "created").extract[String]
    val revisionCreatedEpoch = revisionCreatedDateStr
      .parseStringToUTCSecondsEpoch(AnalyticsDateTimeFormater.yyyy_MM_dd_HHmmss_SSSSSSSSS)
    val revisionOwnerAccountId = (revisionDetails \ "uploader" \ "_account_id").extract[Long]
    val revisionRef = (revisionDetails \ "ref").extract[String]

    Revision("", kindOfRevision, revisionNumber, revisionOwnerAccountId, revisionCreatedEpoch, revisionRef)

  }
}

object ChangeCommentsExtractor extends JsonDataExtractor[FileCommentDetails] {

  override def extractListOfElements(jsonContent: JValue): List[FileCommentDetails] = {
    val commentedFiles = jsonContent.values.asInstanceOf[Map[String, String]].keys

    commentedFiles.map { fileName =>
      val commentsInFile = (jsonContent \ fileName)
      val fileCommentWithoutFileName = extractSingleElement(commentsInFile)
      fileCommentWithoutFileName.copy(fileName)
    }.toList
  }

  override def extractSingleElement(changeCommentsJValue: json4s.JValue): FileCommentDetails = {
    import com.gerritforge.analytics.common.ops.AnalyticsTimeOps.implicits._

    val commentDetails = changeCommentsJValue.extract[List[json4s.JValue]]
    val comments = commentDetails.map { comment =>
      val userAccountId = (comment \ "author" \ "_account_id").extract[String]
      val userFullName = (comment \ "author" \ "name").extract[String]
      val userEmail = (comment \ "author" \ "email").extract[String]
      val username = (comment \ "author" \ "username").extract[String]
      val patchSetNumber = (comment \ "patch_set").extract[Int]

      val commentId = (comment \ "id").extract[String]
      val lineCommented: Int = (comment \ "line").extract[Int]
      val commentUpdateEpoch: Long = (comment \ "updated").extract[String]
        .parseStringToUTCSecondsEpoch(AnalyticsDateTimeFormater.yyyy_MM_dd_HHmmss_SSSSSSSSS)
      val commentMessage: String = (comment \ "message").extract[String]
      val unresolved: Boolean = (comment \ "unresolved").extract[Boolean]

      val commentator = GerritAccountWithId(userAccountId, userFullName, userEmail, username)
      GerritComment(commentId, commentator, unresolved, lineCommented, patchSetNumber, commentUpdateEpoch, commentMessage)
    }

    FileCommentDetails("", comments)
  }
}
