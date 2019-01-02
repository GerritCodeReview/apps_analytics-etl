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

package com.gerritforge.analytics.auditlog.model

import java.time.LocalDate

import com.gerritforge.analytics.support.ops.ReadsOps._
import scopt.OptionParser

object CommandLineArguments {

  def apply(args: Array[String]): Option[AuditLogETLConfig] = {

    val parser: OptionParser[AuditLogETLConfig] =
      new scopt.OptionParser[AuditLogETLConfig]("spark-submit") {
        head("spark-submit")
        opt[String]('u', "gerritUrl") required () action { (input, c) =>
          c.copy(gerritUrl = Some(input))
        } text "gerrit server URL (Required)"

        opt[String]("username") optional () action { (input, c) =>
          c.copy(gerritUsername = Some(input))
        } text "Gerrit API Username (Optional)"

        opt[String]("password") optional () action { (input, c) =>
          c.copy(gerritPassword = Some(input))
        } text "Gerrit API Password (Optional)"

        opt[String]('i', "elasticSearchIndex") required () action { (input, c) =>
          c.copy(elasticSearchIndex = Some(input))
        } text "elasticSearch index to persist data into (Required)"

        opt[String]('p', "eventsPath") required () action { (input, c) =>
          c.copy(eventsPath = Some(input))
        } text "path to a directory (or a file) containing auditLogs events. Supports also '.gz' files. (Required)"

        opt[String]('a', "additionalUserInfoPath") required () action { (input, c) =>
          c.copy(additionalUserInfoPath = Some(input))
        } text "path to a file containing additional user information. (Optional)"

        opt[String]('a', "eventsTimeAggregation") optional () action { (input, c) =>
          c.copy(eventsTimeAggregation = Some(input))
        } text "Events of the same type, produced by the same user will be aggregated with this time granularity: " +
                 "'second', 'minute', 'hour', 'week', 'month', 'quarter'. (Optional) - Default: 'hour'"

        opt[Boolean]('k', "ignoreSSLCert") optional () action { (input, c) =>
          c.copy(ignoreSSLCert = Some(input))
        } text "Ignore SSL certificate validation (Optional) - Default: false"

        opt[LocalDate]('s', "since") optional () action { (input, c) => c.copy(since = Some(input))
        } text "process only auditLogs occurred after (and including) this date, expressed as 'yyyy-MM-dd' (Optional)"

        opt[LocalDate]('u', "until") optional () action { (input, c) => c.copy(until = Some(input))
        } text "process only auditLogs occurred before (and including) this date, expressed as 'yyyy-MM-dd' (Optional)"
      }

      parser.parse(args, AuditLogETLConfig())
  }
}