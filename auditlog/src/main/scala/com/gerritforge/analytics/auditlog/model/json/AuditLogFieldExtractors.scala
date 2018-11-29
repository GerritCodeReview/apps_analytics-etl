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

package com.gerritforge.analytics.auditlog.model.json

import org.json4s.{DefaultFormats, JValue}

trait AuditLogFieldExtractors {
  implicit private val formats = DefaultFormats

  val accessPath  = (jsEvent: JValue) => (jsEvent \ "who" \ "access_path").extractOpt[String]
  val sessionId   = (jsEvent: JValue) => (jsEvent \ "session_id").extract[String]
  val timeAtStart = (jsEvent: JValue) => (jsEvent \ "time_at_start").extract[Long]
  val what        = (jsEvent: JValue) => (jsEvent \ "what").extract[String]
  val elapsed     = (jsEvent: JValue) => (jsEvent \ "elapsed").extract[Int]
  val uuid        = (jsEvent: JValue) => (jsEvent \ "uuid" \ "uuid").extract[String]
  val who         = (jsEvent: JValue) => (jsEvent \ "who" \ "account_id" \ "id").extractOpt[Int]
  val httpMethod  = (jsEvent: JValue) => (jsEvent \ "http_method").extract[String]
  val httpStatus  = (jsEvent: JValue) => (jsEvent \ "http_status").extract[Int]

}
