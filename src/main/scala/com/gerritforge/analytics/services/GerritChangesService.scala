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

package com.gerritforge.analytics.services

import com.gerritforge.analytics.extractors.{ChangeCodeReviewsExtractor, ChangeCommentsExtractor, ChangeHeaderExtractor, ChangeRevisionsExtractor}
import com.gerritforge.analytics.model._
import org.json4s

case class GerritChangesService(apiFetchService: GerritCoreApiFetchService) {


  def buildChangesInformation: List[ChangeInformation] = {

    val fetchedChanges = apiFetchService.fetchChanges(Merged)
    val gerritChanges = ChangeHeaderExtractor.extractListOfElements(fetchedChanges)

    val listOfChangesInformation = gerritChanges.map { change =>

      val changeDetailInfoJValue: json4s.JValue = apiFetchService.fetchChangeDetails(change.urlId)
      val changeCommentsInfoJValue: json4s.JValue = apiFetchService.fetchChangeComments(change.urlId)

      // Extract Gerrit-Change Code Reviews
      val codeReviews: List[CodeReviewScoreInfo] = ChangeCodeReviewsExtractor.extractListOfElements(changeDetailInfoJValue)
      // Extract Gerrit-Change Revisions
      val revisions: List[Revision] = ChangeRevisionsExtractor.extractListOfElements(changeDetailInfoJValue)
      //Extract Gerrit-Change comments
      val comments: List[FileCommentDetails] = ChangeCommentsExtractor.extractListOfElements(changeCommentsInfoJValue)

      ChangeInformation(
        change,
        revisions,
        codeReviews,
        comments
      )
    }

    listOfChangesInformation
  }
}
