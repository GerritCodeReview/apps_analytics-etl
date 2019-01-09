// Copyright (C) 2019 GerritForge Ltd
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

package com.gerritforge.analytics.auditlog.util
import scala.util.matching.Regex

trait RegexUtil {
  def capture(r: String) = new Regex(r, "capture")

  def matches(regex: Regex, string: String): Boolean = regex.findFirstIn(string).isDefined

  def extractOrElse(rx: Regex, target: String, default: String): String = extractGroup(rx, target).getOrElse(default)

  def extractGroup(rx: Regex, target: String): Option[String] = rx.findAllMatchIn(target).toList.headOption.map(_.group("capture"))

}
