// Copyright (C) 2018 GerritForge Ltd
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

package com.gerritforge.analytics.auditlog
import com.gerritforge.analytics.auditlog.model.{HttpAuditEvent, SshAuditEvent}

trait TestFixtures {

  val userId        = 123
  val sessionId     = "someSessionId"
  val gitAccessPath = "GIT"
  val timeAtStart   = 1544802407000L
  val elapsed       = 12
  val uuid          = "audit:5f10fea5-35d1-4252-b86f-99db7a9b549b"
  val project       = "Mirantis/tcp-qa"

  val GIT_UPLOAD_PACK = "git-upload-pack"

  val httpMethod = "GET"
  val httpStatus = "200"

  val httpWhat = s"https://review.gerrithub.io/$project/$GIT_UPLOAD_PACK"

  val anonymousHttpAuditEvent = HttpAuditEvent(
    Some(gitAccessPath),
    httpMethod,
    httpStatus,
    sessionId,
    None,
    timeAtStart,
    httpWhat,
    elapsed,
    uuid
  )
  val authenticatedHttpAuditEvent: HttpAuditEvent = anonymousHttpAuditEvent.copy(who = Some(userId))

  val sshAccessPath                = "SSH_COMMAND"
  val sshResult                    = "0"
  val SSH_GERRIT_COMMAND           = "gerrit"
  val SSH_GERRIT_COMMAND_ARGUMENTS = s"query.--format.json.--current-patch-set.project:$project"

  val sshWhat = s"$SSH_GERRIT_COMMAND.$SSH_GERRIT_COMMAND_ARGUMENTS"

  val sshAuditEvent = SshAuditEvent(
    Some(sshAccessPath),
    sessionId,
    Some(userId),
    timeAtStart,
    sshWhat,
    elapsed,
    uuid,
    sshResult
  )
}
