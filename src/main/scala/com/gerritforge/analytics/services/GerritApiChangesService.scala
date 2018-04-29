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

import java.util

import com.gerritforge.analytics.model._
import com.google.gerrit.extensions.client.ListChangesOption
import com.google.gerrit.extensions.common.{ChangeInfo, CommentInfo}
import com.urswolfer.gerrit.client.rest.GerritRestApi

import scala.collection.JavaConverters._
import scala.collection.mutable

case class GerritApiChangesService(gerritApi: GerritRestApi) {

  private val minimumRequiredOptions = List(ListChangesOption.ALL_REVISIONS, ListChangesOption.DETAILED_LABELS, ListChangesOption.ALL_COMMITS, ListChangesOption.ALL_FILES)

  /**
    * Fetches changes without the comments associated to each change
    *
    * @param changesStatus   - Options New, Merged, Abandoned, Draft
    * @param numberOfChanges - Number of changes fetched from the API
    * @param options         - Changes API options (Default: ALL_REVISIONS, DETAILED_LABELS, ALL_COMMITS)
    * @return List of ChangeInformation with an empty List for the comments
    */
  def fetchChangesWithoutComments(changesStatus: GerritChangeStatus, numberOfChanges: Int, options: List[ListChangesOption]): List[ChangeInfo] = {
    val optionsWithDefault: List[ListChangesOption] = if (options.isEmpty) minimumRequiredOptions else minimumRequiredOptions ++ options

    gerritApi.changes().query(s"status:${changesStatus.stringValue.toLowerCase()}")
      .withOptions(optionsWithDefault: _*)
      .withLimit(numberOfChanges)
      .get().asScala.toList
  }

  /**
    * Fetches changes and the comments associated to each change
    *
    * @param changesStatus   - Options New, Merged, Abandoned, Draft
    * @param numberOfChanges - Number of changes fetched from the API
    * @param options         - Changes API options (Default: ALL_REVISIONS, DETAILED_LABELS, ALL_COMMITS)
    * @return List of ChangeInformation
    */
  def fetchChangesWithComments(changesStatus: GerritChangeStatus, numberOfChanges: Int, options: List[ListChangesOption]): List[(ChangeInfo, mutable.Map[String, util.List[CommentInfo]])] = {
    val changes: List[ChangeInfo] = fetchChangesWithoutComments(changesStatus, numberOfChanges, options)
    changes.map { change =>
      val changeComments: mutable.Map[String, util.List[CommentInfo]] = fetchCommentsForChange(change.id)

      (change, changeComments)
    }
  }

  /**
    * Fetches the comments for a particular change
    *
    * @param id - ID of the change - Format: ProjectName~BranchName~ChangeId
    * @return
    */
  def fetchCommentsForChange(id: String) = gerritApi.changes().id(id).comments().asScala

}
