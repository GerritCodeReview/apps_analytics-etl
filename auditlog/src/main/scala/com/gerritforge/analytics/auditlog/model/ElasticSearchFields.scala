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

object ElasticSearchFields {
  val TIME_BUCKET_FIELD     = "events_time_bucket"
  val COMMAND_FIELD         = "command"
  val COMMAND_ARGS_FIELD    = "command_arguments"
  val USER_IDENTIFIER_FIELD = "user_identifier"
  val AUDIT_TYPE_FIELD      = "audit_type"
  val ACCESS_PATH_FIELD     = "access_path"
  val USER_TYPE_FIELD       = "user_type"

  val FACETING_FIELDS = List(
    TIME_BUCKET_FIELD,
    AUDIT_TYPE_FIELD,
    USER_IDENTIFIER_FIELD,
    USER_TYPE_FIELD,
    ACCESS_PATH_FIELD,
    COMMAND_FIELD,
    COMMAND_ARGS_FIELD
  )

  val NUM_EVENTS_FIELD = "num_events"

  val ALL_DOCUMENT_FIELDS: List[String] = FACETING_FIELDS :+ NUM_EVENTS_FIELD

  val DOCUMENT_TYPE = "auditlog"
}
