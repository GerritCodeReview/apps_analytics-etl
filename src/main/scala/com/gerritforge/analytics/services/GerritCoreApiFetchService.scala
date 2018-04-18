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


package com.gerritforge.analytics.services

import com.gerritforge.analytics.model._
import org.json4s.native.JsonMethods.parse

import scala.io.Source

case class GerritCoreApiFetchService(baseUrl: String) {

  private val cleanedBaseUrl: String = if (baseUrl.last == '/') baseUrl.init else baseUrl

  def cleanupJsonGerritPrefixAndParseToJValue(jsonSource: Source) = {
    val GERRIT_PREFIX = ")]}'\n"
    val GERRIT_PREFIX_LEN = GERRIT_PREFIX.length

    parse(jsonSource.drop(GERRIT_PREFIX_LEN).mkString)
  }

  /**
    * Fetch available changes from Gerrit core API
    *
    * @param changeStatus     - Supported types (Merged, Open, Abandoned, Draft)
    * @param numberOfElements - defines number of elements to return, -1 returns all the elements
    */
  def fetchChanges(changeStatus: GerritChangeStatus, numberOfElements: Int = -1) = {

    val changesBaseUrl = cleanedBaseUrl + "/changes/"

    val projectsUrlBasedOnStatus = changeStatus match {
      case Merged => changesBaseUrl + s"?q=status:${Merged.stringValue}"
      case Open => changesBaseUrl + s"?q=status:${Open.stringValue}"
      case Abandoned => changesBaseUrl + s"?q=status:${Abandoned.stringValue}"
      case Draft => changesBaseUrl + s"?q=status:${Draft.stringValue}"
      case _ => ???
    }

    val projectsUrlBasedOnStatusAndNumberOfElems = numberOfElements match {
      case -1 => projectsUrlBasedOnStatus
      case value => projectsUrlBasedOnStatus + s"&n=${value}"
    }
    val fetchFromUrl = Source.fromURL(projectsUrlBasedOnStatusAndNumberOfElems)

    cleanupJsonGerritPrefixAndParseToJValue(fetchFromUrl)
  }

  /**
    * Fetches the change details, which inlcude ALL_REVISIONS, DETAILED_LABELS and ALL_COMMITS
    *
    * @param urlChangeId - Change URL ID
    */
  def fetchChangeDetails(urlChangeId: String) = {
    val changeDetails = cleanedBaseUrl + s"/changes/${urlChangeId}/?o=ALL_REVISIONS&o=DETAILED_LABELS&o=ALL_COMMITS"

    val fetchFromUrl = Source.fromURL(changeDetails)

    cleanupJsonGerritPrefixAndParseToJValue(fetchFromUrl)
  }

  /**
    * Fetches the all the comments for a gerrit change
    *
    * @param urlChangeId - Change URL ID
    */
  def fetchChangeComments(urlChangeId: String) = {
    val changeComments = cleanedBaseUrl + s"/changes/${urlChangeId}/comments"

    val fetchFromUrl = Source.fromURL(changeComments)
    cleanupJsonGerritPrefixAndParseToJValue(fetchFromUrl)
  }


}