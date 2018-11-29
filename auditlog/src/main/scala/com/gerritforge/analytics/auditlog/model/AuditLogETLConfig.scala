package com.gerritforge.analytics.auditlog.model
import java.time.LocalDate

case class AuditLogETLConfig(
  gerritUrl: Option[String] = None,
  gerritUsername: Option[String] = None,
  gerritPassword: Option[String] = None,
  ignoreSSLCert: Option[Boolean] = None,
  elasticSearchIndex: Option[String] = None,
  since: Option[LocalDate] = None,
  until: Option[LocalDate] = None,
  eventsPath: Option[String] = None,
  eventsTimeAggregation: Option[String] = Some("hour")
)
