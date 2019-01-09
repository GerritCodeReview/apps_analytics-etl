// Copyright (C) 2018 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.analytics.auditlog.broadcast
import java.net.URLDecoder

import com.gerritforge.analytics.auditlog.util.RegexUtil
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.support.ops.GerritSourceOps._
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class GerritProject(name: GerritProjectName)
case class GerritProjects(private val projects: Map[GerritProjectName, GerritProject]) extends LazyLogging with RegexUtil {

  private val PROJECT_SSH_WITH_SPACES           = capture(r = """project:(.+?)\)?\s""")
  private val PROJECT_SSH_WITH_BRACKETS         = capture(r = """project:\{\^?(.*)\}""")
  private val PROJECT_SSH_NO_SPACES             = capture(r = """project(?::|.)(.*)""")
  private val PROJECT_SSH_PACK                  = capture(r = """(?:git-receive-pack|git-upload-pack).\/(.+)""")
  private val PROJECT_SSH_REPLICATION_START     = capture(r = """replication\.start\.(.+)""")
  private val PROJECT_REST_API_CHANGES_SEGMENT  = capture(r = """changes\/([^\/]+)~""")
  private val PROJECT_REST_API_PROJECTS_SEGMENT = capture(r = """projects\/([^\/]+)""")
  private val PROJECT_HTTP_PACK_INFO_REF        = capture(r = """https?:\/\/(.+)\/info\/refs\?service=(?:git-upload-pack|git-receive-pack)""")
  private val PROJECT_HTTP_PACK                 = capture(r = """https?:\/\/(.+)\/(?:git-upload-pack|git-receive-pack)""")

  private val NO_PROJECT_RELATED_COMMANDS = capture(r = """(LOGIN|LOGOUT|AUTH)""")

  private def existProject(id: GerritProjectName): Boolean = projects.get(id).isDefined

  private def findProjectStringAtStart(rawProject: String, sep: Char = '.'): Option[String] =
    rawProject.split(sep).foldLeft(List.empty[String]) { case (acc, token) =>
      acc.headOption.map { t => (t + sep + token) +: acc }.getOrElse(List(token))
    }.find(existProject)

  private def findProjectStringAtEnd(rawProject: String, sep: Char = '.'): Option[String] =
    rawProject.split(sep).foldRight(List.empty[String]) { case (token, acc) =>
      acc.headOption.map { t => (token + sep + t) +: acc }.getOrElse(List(token))
    }.find(existProject)

  def extractProject(what: String, accessPath: String): Option[String] = accessPath match {
    case _ if matches(NO_PROJECT_RELATED_COMMANDS, what) =>
      None
    case "SSH_COMMAND" =>
      extractGroup(PROJECT_SSH_WITH_SPACES, what)
        .orElse(extractGroup(PROJECT_SSH_WITH_BRACKETS, what))
        .orElse(extractGroup(PROJECT_SSH_PACK, what))
        .orElse(extractGroup(PROJECT_SSH_REPLICATION_START, what))
        .orElse(extractGroup(PROJECT_SSH_NO_SPACES, what).flatMap(findProjectStringAtStart(_)))
        .orElse(findProjectStringAtStart(what))
        .orElse(findProjectStringAtEnd(what))
    case "REST_API" | "UNKNOWN" =>
      extractGroup(PROJECT_REST_API_CHANGES_SEGMENT, what)
        .orElse(extractGroup(PROJECT_REST_API_PROJECTS_SEGMENT, what))
        .map(URLDecoder.decode(_, "UTF-8"))
    case "GIT" =>
      extractGroup(PROJECT_HTTP_PACK_INFO_REF, what)
        .orElse(extractGroup(PROJECT_HTTP_PACK, what))
        .flatMap(findProjectStringAtEnd(_, '/'))
    case unexpected =>
      logger.warn(s"Unexpected access path '$unexpected' encountered when extracting project from '$what'")
      None
  }
}

object GerritProjects extends LazyLogging {

  val empty = GerritProjects(Map.empty[GerritProjectName, GerritProject])

  implicit private val formats = DefaultFormats

  def loadProjects(gerritConnectivity: GerritConnectivity, gerritUrl: String): Try[GerritProjects] = {
    val PAGE_SIZE = 500
    logger.debug(s"Loading gerrit projects...")

    val baseUrl = s"""$gerritUrl/projects/?n=$PAGE_SIZE&query=state%3Aactive%20OR%20state%3Aread-only"""

    @tailrec
    def loopThroughPages(more: Boolean, triedAcc: Try[GerritProjects] = Success(empty)): Try[GerritProjects] = {
      if (!more)
        triedAcc
      else {
        val acc = triedAcc.get

        val url              = baseUrl + s"&S=${acc.projects.size}"
        val accountsJsonPage = gerritConnectivity.getContentFromApi(url).dropGerritPrefix.mkString

        logger.debug(s"Getting gerrit projects - start: ${acc.projects.size}")

        val pageInfo = Try(parse(accountsJsonPage)).map { jsMapProjects =>
          val thisPageProjects = jsMapProjects.extract[List[GerritProject]].map(prj => prj.name -> prj)
          (thisPageProjects.size == PAGE_SIZE, acc.copy(projects = acc.projects ++ thisPageProjects))
        }

        pageInfo match {
          case Success((newMore, newGerritProjects)) => loopThroughPages(newMore, Success(newGerritProjects))
          case Failure(exception) => loopThroughPages(more=false, Failure(exception))
        }
      }
    }
    loopThroughPages(more=true)
  }
}
