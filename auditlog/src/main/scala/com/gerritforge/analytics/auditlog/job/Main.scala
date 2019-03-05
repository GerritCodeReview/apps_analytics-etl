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

import com.gerritforge.analytics.auditlog.broadcast.{AdditionalUserInfo, GerritProjects, GerritUserIdentifiers}
import com.gerritforge.analytics.auditlog.model.ElasticSearchFields._
import com.gerritforge.analytics.auditlog.model._
import com.gerritforge.analytics.auditlog.range.TimeRange
import com.gerritforge.analytics.auditlog.spark.rdd.ops.SparkRDDOps._
import com.gerritforge.analytics.auditlog.spark.session.ops.SparkSessionOps._
import com.gerritforge.analytics.common.api.GerritConnectivity
import com.gerritforge.analytics.spark.SparkApp
import com.typesafe.scalalogging.LazyLogging

object Main extends SparkApp with App with LazyLogging {
  override val appName = "Gerrit AuditLog Analytics ETL"

  CommandLineArguments(args) match {
    case Some(config) =>

      val tryProjects = GerritProjects.loadProjects(
        new GerritConnectivity(config.gerritUsername, config.gerritPassword, config.ignoreSSLCert.getOrElse(false)),
        config.gerritUrl.get
      )

      if (tryProjects.isFailure) {
        logger.error("Error loading public projects", tryProjects.failed.get)
        sys.exit(1)
      }

      val tryUserIdentifiers = GerritUserIdentifiers.loadAccounts(
        new GerritConnectivity(config.gerritUsername, config.gerritPassword, config.ignoreSSLCert.getOrElse(false)),
        config.gerritUrl.get
      )

      if (tryUserIdentifiers.isFailure) {
        logger.error("Error loading gerrit user identifiers", tryUserIdentifiers.failed.get)
        sys.exit(1)
      }

      val triedAdditionalUserInfo = AdditionalUserInfo.loadAdditionalUserInfo(config)
      if (triedAdditionalUserInfo.isFailure) {
        logger.error("Error loading additional user information", triedAdditionalUserInfo.failed.get)
        sys.exit(1)
      }

      import com.gerritforge.analytics.infrastructure.ESSparkWriterImplicits.withAliasSwap
      import scala.concurrent.ExecutionContext.Implicits.global
      spark
        .getEventsFromPath(config.eventsPath.get)
        .transformEvents(
          tryUserIdentifiers.get,
          triedAdditionalUserInfo.get,
          tryProjects.get,
          config.eventsTimeAggregation.get,
          TimeRange(config.since, config.until)
        )
        .saveToEsWithAliasSwap(config.elasticSearchIndex.get, DOCUMENT_TYPE)
        .futureAction
        .map(actionRespose => logger.info(s"Completed index swap ${actionRespose}"))
        .recover { case exception: Exception => logger.info(s"Index swap failed ${exception}") }

    case None =>
      logger.error("Could not parse command line arguments")
      sys.exit(1)
  }

}
