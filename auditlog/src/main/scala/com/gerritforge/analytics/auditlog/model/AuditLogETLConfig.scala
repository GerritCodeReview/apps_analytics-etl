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

case class AuditLogETLConfig(
  gerritUrl: Option[String] = None,
  gerritUsername: Option[String] = None,
  gerritPassword: Option[String] = None,
  ignoreSSLCert: Option[Boolean] = None,
  elasticSearchIndex: Option[String] = None,
  since: Option[LocalDate] = None,
  until: Option[LocalDate] = None,
  eventsPath: Option[String] = None,
  eventsTimeAggregation: Option[String] = Some("hour"),
  additionalUserInfoPath: Option[String] = None
)
