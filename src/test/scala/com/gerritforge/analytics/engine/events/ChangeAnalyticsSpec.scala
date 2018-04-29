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

import java.util

import com.gerritforge.analytics.engine.ChangesAnalyticsProcessor
import com.gerritforge.analytics.model.{ChangeInformation, ChangeStatusOps, Revision}
import com.gerritforge.analytics.support.DefaultValuesSupport.GerritModelDefaultValues
import com.gerritforge.analytics.support.SparkTestSupport
import com.google.gerrit.extensions.common.{ChangeInfo, CommentInfo}
import org.apache.spark.sql.Dataset
import org.scalatest.{Assertion, Matchers, WordSpec}

import scala.collection.JavaConverters._
import scala.collection.mutable

class ChangeAnalyticsSpec extends WordSpec with Matchers with SparkTestSupport {

  "Changes" when {
    "have not comments" should {
      "an empty List" in {
        import GerritModelDefaultValues._
        val changeInfo1 = defaultChangeInfo()
        val changeInfo2 = defaultChangeInfo(id = "Project2")

        val changes = List(changeInfo1, changeInfo2)

        import ChangesAnalyticsProcessor._
        val changeInformation: Dataset[ChangeInformation] = convertToAnalyticsDataModelWithoutComments(changes)(spark)

        import spark.implicits._
        changeInformation
          .map(_.filesCommented).collect().fold(List.empty)(_ union _) should equal(List.empty)
      }

      "have a total of 3 comments" should {
        "returns a List with 3 comments" in {
          import GerritModelDefaultValues._
          val changeInfo1 = defaultChangeInfo()
          val changeInfo2 = defaultChangeInfo(id = "Project2")

          val comment1 = defaultCommentInfo()
          val comment2 = defaultCommentInfo(line = 2, message = "Default Comment 2")
          val comment3 = defaultCommentInfo(line = 3, message = "Default Comment 3")

          val numberOfComments = 3

          val commentsPerFileForChange1 = mutable.Map("file1" -> List(comment1).asJava, "file2" -> List(comment2).asJava)
          val commentsPerFileForChange2 = mutable.Map("file1" -> List(comment1, comment3).asJava)

          val changeInfoWithComments: List[(ChangeInfo, mutable.Map[String, util.List[CommentInfo]])] = List((changeInfo1, commentsPerFileForChange1), (changeInfo2, commentsPerFileForChange2))

          import ChangesAnalyticsProcessor._
          val changeInformation: Dataset[ChangeInformation] = convertToAnalyticsDataModel(changeInfoWithComments)(spark)

          import spark.implicits._
          changeInformation
            .map(_.filesCommented)
            .collect()
            .fold(List.empty)(_ union _)
            .size should equal(numberOfComments)
        }
      }
    }
  }

  "A ChangeInfo from Gerrit API" when {
    import GerritModelDefaultValues._
    val changeInfo = defaultChangeInfo()

    "converted to ChangeInformation without comments" should {
      import ChangesAnalyticsProcessor._
      val changeInformationWithoutComments: ChangeInformation = convertToAnalyticsDataModelWithoutComments(List(changeInfo))(spark).head

      "match fields mapping" in {
        checkCommonChangeFields(changeInfo, changeInformationWithoutComments)
      }
    }

    "converted to ChangeInformation with two comments" should {
      import ChangesAnalyticsProcessor._
      import GerritModelDefaultValues._

      val commentOne = defaultCommentInfo()
      val commentTwo = defaultCommentInfo(path = "file2", line = 2, message = "Default Comment 2")

      val commentPerFileForChange1 = mutable.Map("file1" -> List(commentOne).asJava, "file2" -> List(commentTwo).asJava)
      val changeInfoWithTwoComments: List[(ChangeInfo, mutable.Map[String, util.List[CommentInfo]])] = List((changeInfo, commentPerFileForChange1))

      val changeInformationWithTwoComments: ChangeInformation = convertToAnalyticsDataModel(changeInfoWithTwoComments)(spark).head

      "match fields mapping" in {
        checkCommonChangeFields(changeInfo, changeInformationWithTwoComments)

        changeInformationWithTwoComments.filesCommented.size should equal(2)
        val commentOneAfterConversion = changeInformationWithTwoComments.filesCommented.filter(_.fileName != "file2").head
        val commentTwoAfterConversion = changeInformationWithTwoComments.filesCommented.filter(_.fileName == "file2").head

        commentOne.message should equal(commentOneAfterConversion.comments.head.message)
        commentOne.line should equal(commentOneAfterConversion.comments.head.lineNumber)
        commentOne.id should equal(commentOneAfterConversion.comments.head.id)

        commentTwo.message should equal(commentTwoAfterConversion.comments.head.message)
        commentTwo.line should equal(commentTwoAfterConversion.comments.head.lineNumber)
        commentTwo.id should equal(commentTwoAfterConversion.comments.head.id)

      }
    }
  }
  "A Change" when {
    import GerritModelDefaultValues._
    val changeInfo = defaultChangeInfo()

    "has 2 comments" should {
      val commentOne = defaultCommentInfo()
      val commentTwo = defaultCommentInfo(path = "file2", line = 2, message = "Default Comment 2")

      "return 2" in {
        import ChangesAnalyticsProcessor._
        val commentPerFileForChange1 = mutable.Map("file1" -> List(commentOne).asJava, "file2" -> List(commentTwo).asJava)
        val changeInfoWithTwoComments: List[(ChangeInfo, mutable.Map[String, util.List[CommentInfo]])] = List((changeInfo, commentPerFileForChange1))

        val changeInformationWithTwoComments: ChangeInformation = convertToAnalyticsDataModel(changeInfoWithTwoComments)(spark).head

        calculateNumberOfComments(changeInformationWithTwoComments) should equal(2)
      }

    }
  }

  "Calculates maximum number from a list" in {
    val list = List(1, 2, 3, 4, 5)
    val maxValue = ChangesAnalyticsProcessor.calculateMaxFromList(list)

    maxValue should equal(5)
  }


  def checkCommonChangeFields(changeInfo: ChangeInfo, changeInformation: ChangeInformation): Assertion = {
    changeInfo.id should equal(changeInformation.id)
    changeInfo._number should equal(changeInformation.changeNumber)
    changeInfo.changeId should equal(changeInformation.changeId)
    changeInfo.insertions should equal(changeInformation.insertions)
    changeInfo.deletions should equal(changeInformation.deletions)
    changeInfo.branch should equal(changeInformation.branch)
    changeInfo.owner._accountId should equal(changeInformation.changeOwnerAccountId)
    changeInfo.unresolvedCommentCount should equal(changeInformation.unresolvedCommentCount)
    changeInfo.hasReviewStarted should equal(changeInformation.hasReviewStarted)

    ChangeStatusOps.fromEnumChangeStatusToChangeStatus(changeInfo.status) should equal(changeInformation.status)
    Revision.fromGerritChangeInfo(changeInfo) should equal(changeInformation.revisions)
  }
}
