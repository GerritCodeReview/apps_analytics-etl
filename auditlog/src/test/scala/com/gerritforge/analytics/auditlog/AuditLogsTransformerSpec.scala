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
import java.sql

import com.gerritforge.analytics.SparkTestSupport
import com.gerritforge.analytics.auditlog.broadcast.GerritUserIdentifiers
import com.gerritforge.analytics.auditlog.model.{ElasticSearchFields, HttpAuditEvent, SshAuditEvent}
import com.gerritforge.analytics.auditlog.spark.AuditLogsTransformer
import com.gerritforge.analytics.support.ops.CommonTimeOperations._
import org.apache.spark.sql.Row
import org.scalatest.{FlatSpec, Matchers}

class AuditLogsTransformerSpec extends FlatSpec with Matchers with SparkTestSupport with TestFixtures {
  behavior of "AuditLogsTransformer"

  it should "process an anonymous http audit entry" in {
    val events = Seq(anonymousHttpAuditEvent)

    val dataFrame = AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation="hour")

    dataFrame.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    val expectedAggregatedCount = 1
    dataFrame.collect should contain only Row(
        AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
        HttpAuditEvent.auditType,
        null, // no user identifier
        anonymousHttpAuditEvent.accessPath.get,
        GIT_UPLOAD_PACK,
        anonymousHttpAuditEvent.what,
        anonymousHttpAuditEvent.result,
        expectedAggregatedCount
    )
  }

  it should "process an authenticated http audit entry where gerrit account couldn't be identified" in {
    val events = Seq(authenticatedHttpAuditEvent)

    val dataFrame = AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation="hour")

    dataFrame.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    val expectedAggregatedCount = 1
    dataFrame.collect should contain only Row(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      HttpAuditEvent.auditType,
      s"${authenticatedHttpAuditEvent.who.get}",
      authenticatedHttpAuditEvent.accessPath.get,
      GIT_UPLOAD_PACK,
      authenticatedHttpAuditEvent.what,
      authenticatedHttpAuditEvent.result,
      expectedAggregatedCount
    )
  }

  it should "process an authenticated http audit entry where gerrit account could be identified" in {
    val events = Seq(authenticatedHttpAuditEvent)
    val gerritUserIdentifier = "Antonio Barone"

    val dataFrame =
      AuditLogsTransformer(GerritUserIdentifiers(Map(authenticatedHttpAuditEvent.who.get -> gerritUserIdentifier)))
        .transform(sc.parallelize(events), timeAggregation="hour")

    dataFrame.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    val expectedAggregatedCount = 1
    dataFrame.collect should contain only Row(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      HttpAuditEvent.auditType,
      gerritUserIdentifier,
      authenticatedHttpAuditEvent.accessPath.get,
      GIT_UPLOAD_PACK,
      authenticatedHttpAuditEvent.what,
      authenticatedHttpAuditEvent.result,
      expectedAggregatedCount
    )
  }

  it should "process an SSH audit entry" in {
    val events = Seq(sshAuditEvent)

    val dataFrame = AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation="hour")

    dataFrame.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    val expectedAggregatedCount = 1
    dataFrame.collect should contain only Row(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      SshAuditEvent.auditType,
      s"${sshAuditEvent.who.get}",
      sshAuditEvent.accessPath.get,
      SSH_GERRIT_COMMAND,
      SSH_GERRIT_COMMAND_ARGUMENTS,
      sshAuditEvent.result,
      expectedAggregatedCount
    )
  }

  it should "group ssh events from the same user together, if they fall within the same time bucket (hour)" in {
    val events = Seq(sshAuditEvent, sshAuditEvent.copy(timeAtStart = timeAtStart + 1000))

    val dataFrame = AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation="hour")

    dataFrame.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    val expectedAggregatedCount = 2
    dataFrame.collect should contain only Row(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      SshAuditEvent.auditType,
      s"${sshAuditEvent.who.get}",
      sshAuditEvent.accessPath.get,
      SSH_GERRIT_COMMAND,
      SSH_GERRIT_COMMAND_ARGUMENTS,
      sshAuditEvent.result,
      expectedAggregatedCount
    )
  }

  it should "group ssh events from different users separately, even if they fall within the same time bucket (hour)" in {
    val user2Id = sshAuditEvent.who.map(_ + 1)
    val events = Seq(sshAuditEvent, sshAuditEvent.copy(who=user2Id))

    val dataFrame = AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation="hour")

    dataFrame.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    val expectedAggregatedCount = 1
    dataFrame.collect should contain allOf (
      Row(
        AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
        SshAuditEvent.auditType,
        s"${sshAuditEvent.who.get}",
        sshAuditEvent.accessPath.get,
        SSH_GERRIT_COMMAND,
        SSH_GERRIT_COMMAND_ARGUMENTS,
        sshAuditEvent.result,
        expectedAggregatedCount
      ),
      Row(
        AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
        SshAuditEvent.auditType,
        s"${user2Id.get}",
        sshAuditEvent.accessPath.get,
        SSH_GERRIT_COMMAND,
        SSH_GERRIT_COMMAND_ARGUMENTS,
        sshAuditEvent.result,
        expectedAggregatedCount
      )
    )
  }

  it should "group different event types separately, event if they fall within the same time bucket (hour)" in {
    val events = Seq(sshAuditEvent, authenticatedHttpAuditEvent.copy(timeAtStart = sshAuditEvent.timeAtStart + 1000))

    val dataFrame = AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation="hour")

    dataFrame.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    val expectedSshAggregatedCount = 1
    val expectedHttpAggregatedCount = 1
    dataFrame.collect should contain allOf (
      Row(
        AuditLogsTransformerSpec.epochMillisToNearestHour(events.head.timeAtStart),
        SshAuditEvent.auditType,
        s"${sshAuditEvent.who.get}",
        sshAuditEvent.accessPath.get,
        SSH_GERRIT_COMMAND,
        SSH_GERRIT_COMMAND_ARGUMENTS,
        sshAuditEvent.result,
        expectedSshAggregatedCount
      ),
      Row(
        AuditLogsTransformerSpec.epochMillisToNearestHour(events.last.timeAtStart),
        HttpAuditEvent.auditType,
        s"${authenticatedHttpAuditEvent.who.get}",
        authenticatedHttpAuditEvent.accessPath.get,
        GIT_UPLOAD_PACK,
        authenticatedHttpAuditEvent.what,
        authenticatedHttpAuditEvent.result,
        expectedHttpAggregatedCount
      )
    )
  }
}

object AuditLogsTransformerSpec {
  def epochMillisToNearestHour(epochMillis: Long): sql.Timestamp = {
    val millisInOneHour = 3600000
    epochToSqlTimestampOps(epochMillis - (epochMillis % millisInOneHour))
  }
}
