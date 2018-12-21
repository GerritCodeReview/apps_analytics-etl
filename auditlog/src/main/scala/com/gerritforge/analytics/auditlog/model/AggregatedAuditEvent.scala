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

package com.gerritforge.analytics.auditlog.model
import java.sql.Timestamp

case class AggregatedAuditEvent(
    events_time_bucket: Timestamp,
    audit_type: String,
    user_identifier: Option[String],
    user_type: Option[String],
    access_path: Option[String],
    command: String,
    command_arguments: String,
    sub_command: Option[String],
    project: Option[String],
    result: String,
    num_events: Long
)
