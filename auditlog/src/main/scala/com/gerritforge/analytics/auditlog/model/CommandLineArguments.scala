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
        } text "gerrit URL"

        opt[String]('i', "elasticSearchIndex") required () action { (input, c) =>
          c.copy(elasticSearchIndex = Some(input))
        } text "elasticSearch URL"

        opt[String]('p', "eventsDirectoryPath") required () action { (input, c) =>
          c.copy(eventsDirectoryPath = Some(input))
        } text "path to a directory or a file containing auditlogs events"

        opt[String]('a', "eventsTimeAggregation") optional () action { (input, c) =>
          c.copy(eventsTimeAggregation = Some(input))
        } text "time aggregation granularity: 'second', 'minute', 'hour', 'week', 'month', 'quarter'. Default 'hour'"

        opt[String]("username") optional () action { (input, c) =>
          c.copy(gerritUsername = Some(input))
        } text "Gerrit API Username"

        opt[String]("password") optional () action { (input, c) =>
          c.copy(gerritPassword = Some(input))
        } text "Gerrit API Password"

        opt[Boolean]('k', "ignoreSSLCert") optional () action { (input, c) =>
          c.copy(ignoreSSLCert = Some(input))
        } text "Ignore SSL certificate validation"

        opt[LocalDate]('s', "since") optional () action { (input, c) => c.copy(since = Some(input))
        } text "begin date "

        opt[LocalDate]('u', "until") optional () action { (input, c) => c.copy(until = Some(input))
        } text "until date"
      }

      parser.parse(args, AuditLogETLConfig())
  }
}