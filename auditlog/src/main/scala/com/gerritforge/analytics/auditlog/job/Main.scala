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

package com.gerritforge.analytics.auditlog.job

import com.gerritforge.analytics.auditlog.broadcast.GerritUserIdentifiers
import com.gerritforge.analytics.auditlog.model.ElasticSearchFields._
import com.gerritforge.analytics.auditlog.model._
import com.gerritforge.analytics.auditlog.range.TimeRange
import com.gerritforge.analytics.auditlog.spark.AuditLogsAggregator
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.spark.SparkApp
import com.typesafe.scalalogging.LazyLogging
import org.elasticsearch.spark.sql._

object Main extends SparkApp with App with LazyLogging {
  override val appName = "Gerrit AuditLog Analytics ETL"

  CommandLineArguments(args) match {
    case Some(config) =>
      val tryGerritAccounts = GerritUserIdentifiers.loadAccounts(
        new GerritConnectivity(config.gerritUsername, config.gerritPassword, config.ignoreSSLCert.getOrElse(false)),
        config.gerritUrl.get
      )

      if(tryGerritAccounts.isFailure) {
        logger.error("Error loading gerrit user identifiers", tryGerritAccounts.failed.get)
        sys.exit(1)
      }

      AuditLogsAggregator(tryGerritAccounts.get)
        .aggregateLogs(
          logPath         = config.eventsPath.get,
          timeAggregation = config.eventsTimeAggregation.get,
          timeRange       = TimeRange(config.since, config.until)
        )
        .saveToEs(s"${config.elasticSearchIndex.get}/$DOCUMENT_TYPE")
    case None =>
      logger.error("Could not parse command line arguments")
      sys.exit(1)
  }
}

