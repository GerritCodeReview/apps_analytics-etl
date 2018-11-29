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

package com.gerritforge.analytics.support.ops

import java.time.LocalDate

import scopt.Read
import scopt.Read.reads

import scala.util.control.NonFatal

object ReadsOps {

  implicit val localDateRead: Read[LocalDate] = reads { dateStr =>
    val cliDateFormat = AnalyticsDateTimeFormatter.yyyy_MM_dd
    try {
      import com.gerritforge.analytics.support.ops.implicits._
      dateStr.parseStringToLocalDate(cliDateFormat).get
    } catch {
      case NonFatal(e) =>
        throw new IllegalArgumentException(
          s"Invalid date '$dateStr' expected format is '${cliDateFormat}'", e)
    }
  }
}
