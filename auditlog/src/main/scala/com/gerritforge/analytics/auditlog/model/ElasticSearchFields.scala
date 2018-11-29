package com.gerritforge.analytics.auditlog.model

object ElasticSearchFields {
  val TIME_BUCKET_FIELD     = "events_time_bucket"
  val COMMAND_FIELD         = "command"
  val COMMAND_ARGS_FIELD    = "command_arguments"
  val USER_IDENTIFIER_FIELD = "user_identifier"
  val AUDIT_TYPE_FIELD      = "audit_type"
  val ACCESS_PATH_FIELD     = "access_path"

  val FACETING_FIELDS = List(
    TIME_BUCKET_FIELD,
    AUDIT_TYPE_FIELD,
    USER_IDENTIFIER_FIELD,
    ACCESS_PATH_FIELD,
    COMMAND_FIELD,
    COMMAND_ARGS_FIELD
  )

  val NUM_EVENTS_FIELD = "num_events"

  val DOCUMENT_TYPE = "auditlog"
}
