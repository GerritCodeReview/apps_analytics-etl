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

package com.gerritforge.analytics.auditlog.spark.sql.udf

import com.gerritforge.analytics.auditlog.util.RegexUtil
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf

case object SparkExtractors extends LazyLogging with RegexUtil {

  // regular expressions to extract commands
  private val GERRIT_SSH_COMMAND = capture(r = """^(.+?)\.""")
  private val GIT_COMMAND        = capture(r = """.*(git-upload-pack|git-receive-pack)""")

  // regular expressions to extract command arguments
  private val GERRIT_SSH_COMMAND_ARGUMENTS = capture(r = """^.+?\.(.+)""")
  private val GIT_SSH_COMMAND_ARGUMENTS    = capture(r = """git-(?:upload|receive)-pack\.(.+)""")
  private val GIT_HTTP_COMMAND_ARGUMENTS   = capture(r = """(^http.*)""")

  // regular expressions to extract sub-commands
  // Rest API sub-command example: what = /config/server/version -> sub-command: config
  private val REST_API_SUB_COMMAND = capture("""^\/(.*?)(?:\/|\s|$)""")
  // SSH sub-command example: what = gerrit.plugin.reload.analytics -> sub-command: plugin
  private val SSH_SUB_COMMAND = capture("""^.*?\.(.*?)(?:\.|\s|$)""")

  val FAILED_SSH_AUTH = "FAILED_SSH_AUTH"

  def extractCommand(what: String, accessPath: String, httpMethod: String = null): String = accessPath match {
    case "SSH_COMMAND"          => extractOrElse(GERRIT_SSH_COMMAND, what, what)
    case "GIT"                  => extractOrElse(GIT_COMMAND, what, what)
    case "REST_API"|"UNKNOWN"   => Option(httpMethod).getOrElse(what)
    case "JSON_RPC"             => what
    case null if what == "AUTH" => FAILED_SSH_AUTH
    case unexpected             =>
      logger.warn(s"Unexpected access path '$unexpected' encountered when extracting command from '$what'")
      what
  }

  def extractCommandUDF: UserDefinedFunction = udf((rawCommand: String, accessPath: String, httpMethod: String) => extractCommand(rawCommand, accessPath, httpMethod))

  def extractCommandArguments(what: String, accessPath: String): Option[String] = accessPath match {
    case "SSH_COMMAND"          => extractGroup(GERRIT_SSH_COMMAND_ARGUMENTS, what)
    case "GIT"                  => Option(extractGroup(GIT_SSH_COMMAND_ARGUMENTS, what).getOrElse(extractOrElse(GIT_HTTP_COMMAND_ARGUMENTS, what, null)))
    case "REST_API"|"UNKNOWN"   => Some(what)
    case "JSON_RPC"             => None
    case null if what == "AUTH" => None
    case unexpected             =>
      logger.warn(s"Unexpected access path '$unexpected' encountered when extracting command arguments from '$what'")
      None
  }

  def extractCommandArgumentsUDF: UserDefinedFunction = udf((rawCommand: String, accessPath: String) => extractCommandArguments(rawCommand, accessPath))

  def extractSubCommand(what: String, accessPath: String): Option[String] = accessPath match {
    case "REST_API"|"UNKNOWN"   => Some(extractOrElse(REST_API_SUB_COMMAND, what, what))
    case "SSH_COMMAND"          => Some(extractOrElse(SSH_SUB_COMMAND, what, what))
    case "GIT"                  => None
    case "JSON_RPC"             => None
    case unexpected             =>
      logger.warn(s"Unexpected access path '$unexpected' encountered when extracting command from '$what'")
      None
  }

  def extractSubCommandUDF: UserDefinedFunction = udf((rawCommand: String, accessPath: String) => extractSubCommand(rawCommand, accessPath))
}