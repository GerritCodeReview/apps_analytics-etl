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

import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf

import scala.util.matching.Regex

case object SparkExtractors {
  private val GERRIT_SSH_COMMAND = new Regex("""^(.+?)\.""", "capture")
  private val GERRIT_SSH_COMMAND_ARGUMENTS = new Regex("""^.+?\.(.+)""", "capture")

  private val GIT_COMMAND = new Regex(""".*(git-upload-pack|git-receive-pack)""", "capture")
  private val GIT_SSH_COMMAND_ARGUMENTS = new Regex("""git-(?:upload|receive)-pack\.(.+)""", "capture")
  private val GIT_HTTP_COMMAND_ARGUMENTS = new Regex("""(^http.*)""", "capture")

  val FAILED_SSH_AUTH = "FAILED_SSH_AUTH"

  private def extractOrElse(rx: Regex, target: String, default: String): String = extractGroup(rx, target).getOrElse(default)

  private def extractGroup(rx: Regex, target: String): Option[String] = rx.findAllMatchIn(target).toList.headOption.map(_.group("capture"))

  def extractCommand(what: String, accessPath: String, httpMethod: String = null): String = accessPath match {
    case "SSH_COMMAND"          => extractOrElse(GERRIT_SSH_COMMAND, what, what)
    case "GIT"                  => extractOrElse(GIT_COMMAND, what, what)
    case "REST_API"|"UNKNOWN"   => Option(httpMethod).getOrElse(what)
    case "JSON_RPC"             => what
    case null if what == "AUTH" => FAILED_SSH_AUTH
    case unexpected             => throw new IllegalArgumentException(s"Unexpected access path $unexpected. Cannot extract command from '$what'")
  }

  def extractCommandUDF: UserDefinedFunction = udf((rawCommand: String, accessPath: String, httpMethod: String) => extractCommand(rawCommand, accessPath, httpMethod))

  def extractCommandArguments(what: String, accessPath: String): Option[String] = accessPath match {
    case "SSH_COMMAND"          => extractGroup(GERRIT_SSH_COMMAND_ARGUMENTS, what)
    case "GIT"                  => Option(extractGroup(GIT_SSH_COMMAND_ARGUMENTS, what).getOrElse(extractOrElse(GIT_HTTP_COMMAND_ARGUMENTS, what, null)))
    case "REST_API"|"UNKNOWN"   => Some(what)
    case "JSON_RPC"             => None
    case null if what == "AUTH" => None
    case unexpected             => throw new IllegalArgumentException(s"Unexpected access path $unexpected. Cannot extract command arguments from '$what'")
  }

  def extractCommandArgumentsUDF: UserDefinedFunction = udf((rawCommand: String, accessPath: String) => extractCommandArguments(rawCommand, accessPath))
}