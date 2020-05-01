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
import com.gerritforge.analytics.auditlog.broadcast._
import com.gerritforge.analytics.auditlog.model.{
  AggregatedAuditEvent,
  ElasticSearchFields,
  HttpAuditEvent,
  SshAuditEvent
}
import com.gerritforge.analytics.auditlog.spark.AuditLogsTransformer
import com.gerritforge.analytics.support.ops.CommonTimeOperations._
import org.scalatest.{FlatSpec, Matchers}

class AuditLogsTransformerSpec
    extends FlatSpec
    with Matchers
    with SparkTestSupport
    with TestFixtures {
  behavior of "AuditLogsTransformer"

  it should "process an anonymous http audit entry" in {
    val events = Seq(anonymousHttpAuditEvent)

    val aggregatedEventsDS =
      AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    aggregatedEventsDS.collect should contain only AggregatedAuditEvent(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      HttpAuditEvent.auditType,
      None,
      None,
      anonymousHttpAuditEvent.accessPath,
      GIT_UPLOAD_PACK,
      anonymousHttpAuditEvent.what,
      None,
      None,
      anonymousHttpAuditEvent.result,
      num_events = 1
    )
  }

  it should "process an authenticated http audit entry where gerrit account couldn't be identified" in {
    val events = Seq(authenticatedHttpAuditEvent)

    val aggregatedEventsDS =
      AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    aggregatedEventsDS.collect should contain only AggregatedAuditEvent(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      HttpAuditEvent.auditType,
      authenticatedHttpAuditEvent.who.map(_.toString),
      Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
      anonymousHttpAuditEvent.accessPath,
      GIT_UPLOAD_PACK,
      anonymousHttpAuditEvent.what,
      None,
      None,
      anonymousHttpAuditEvent.result,
      num_events = 1
    )
  }

  it should "process an authenticated http audit entry where gerrit account could be identified" in {
    val events               = Seq(authenticatedHttpAuditEvent)
    val gerritUserIdentifier = "Antonio Barone"

    val aggregatedEventsDS =
      AuditLogsTransformer(
        GerritUserIdentifiers(Map(authenticatedHttpAuditEvent.who.get -> gerritUserIdentifier))
      ).transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    aggregatedEventsDS.collect should contain only AggregatedAuditEvent(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      HttpAuditEvent.auditType,
      Some(gerritUserIdentifier),
      Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
      anonymousHttpAuditEvent.accessPath,
      GIT_UPLOAD_PACK,
      authenticatedHttpAuditEvent.what,
      None,
      None,
      authenticatedHttpAuditEvent.result,
      num_events = 1
    )
  }

  it should "process an SSH audit entry" in {
    val events = Seq(sshAuditEvent)

    val aggregatedEventsDS =
      AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    aggregatedEventsDS.collect should contain only AggregatedAuditEvent(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      SshAuditEvent.auditType,
      sshAuditEvent.who.map(_.toString),
      Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
      sshAuditEvent.accessPath,
      SSH_GERRIT_COMMAND,
      SSH_GERRIT_COMMAND_ARGUMENTS,
      Some("query"),
      None,
      sshAuditEvent.result,
      num_events = 1
    )
  }

  it should "group ssh events from the same user together, if they fall within the same time bucket (hour)" in {
    val events = Seq(sshAuditEvent, sshAuditEvent.copy(timeAtStart = timeAtStart + 1000))

    val aggregatedEventsDS =
      AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    aggregatedEventsDS.collect should contain only AggregatedAuditEvent(
      AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
      SshAuditEvent.auditType,
      sshAuditEvent.who.map(_.toString),
      Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
      sshAuditEvent.accessPath,
      SSH_GERRIT_COMMAND,
      SSH_GERRIT_COMMAND_ARGUMENTS,
      Some("query"),
      None,
      sshAuditEvent.result,
      num_events = 2
    )
  }

  it should "group ssh events from different users separately, even if they fall within the same time bucket (hour)" in {
    val user2Id = sshAuditEvent.who.map(_ + 1)
    val events  = Seq(sshAuditEvent, sshAuditEvent.copy(who = user2Id))

    val aggregatedEventsDS =
      AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    aggregatedEventsDS.collect should contain allOf (
      AggregatedAuditEvent(
        AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
        SshAuditEvent.auditType,
        sshAuditEvent.who.map(_.toString),
        Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
        sshAuditEvent.accessPath,
        SSH_GERRIT_COMMAND,
        SSH_GERRIT_COMMAND_ARGUMENTS,
        Some("query"),
        None,
        sshAuditEvent.result,
        num_events = 1
      ),
      AggregatedAuditEvent(
        AuditLogsTransformerSpec.epochMillisToNearestHour(timeAtStart),
        SshAuditEvent.auditType,
        user2Id.map(_.toString),
        Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
        sshAuditEvent.accessPath,
        SSH_GERRIT_COMMAND,
        SSH_GERRIT_COMMAND_ARGUMENTS,
        Some("query"),
        None,
        sshAuditEvent.result,
        num_events = 1
      )
    )
  }

  it should "group different event types separately, event if they fall within the same time bucket (hour)" in {
    val events = Seq(
      sshAuditEvent,
      authenticatedHttpAuditEvent.copy(timeAtStart = sshAuditEvent.timeAtStart + 1000)
    )

    val aggregatedEventsDS =
      AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.columns should contain theSameElementsAs ElasticSearchFields.ALL_DOCUMENT_FIELDS

    aggregatedEventsDS.collect should contain allOf (
      AggregatedAuditEvent(
        AuditLogsTransformerSpec.epochMillisToNearestHour(events.head.timeAtStart),
        SshAuditEvent.auditType,
        sshAuditEvent.who.map(_.toString),
        Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
        sshAuditEvent.accessPath,
        SSH_GERRIT_COMMAND,
        SSH_GERRIT_COMMAND_ARGUMENTS,
        Some("query"),
        None,
        sshAuditEvent.result,
        num_events = 1
      ),
      AggregatedAuditEvent(
        AuditLogsTransformerSpec.epochMillisToNearestHour(events.last.timeAtStart),
        HttpAuditEvent.auditType,
        authenticatedHttpAuditEvent.who.map(_.toString),
        Some(AdditionalUserInfo.DEFAULT_USER_TYPE),
        authenticatedHttpAuditEvent.accessPath,
        GIT_UPLOAD_PACK,
        authenticatedHttpAuditEvent.what,
        None,
        None,
        authenticatedHttpAuditEvent.result,
        num_events = 1
      )
    )
  }

  it should "process user type" in {
    val events = Seq(authenticatedHttpAuditEvent)

    val userType           = "nonDefaultUserType"
    val additionalUserInfo = AdditionalUserInfo(authenticatedHttpAuditEvent.who.get, userType)

    val aggregatedEventsDS = AuditLogsTransformer(
      additionalUsersInfo =
        AdditionalUsersInfo(Map(authenticatedHttpAuditEvent.who.get -> additionalUserInfo))
    ).transform(
      auditEventsRDD = sc.parallelize(events),
      timeAggregation = "hour"
    )
    aggregatedEventsDS.collect.length shouldBe 1
    aggregatedEventsDS.collect.head.user_type should contain(userType)
  }

  it should "process user type when gerrit account could be identified" in {
    val events               = Seq(authenticatedHttpAuditEvent)
    val gerritUserIdentifier = "Antonio Barone"

    val userType           = "nonDefaultUserType"
    val additionalUserInfo = AdditionalUserInfo(authenticatedHttpAuditEvent.who.get, userType)

    val aggregatedEventsDS =
      AuditLogsTransformer(
        gerritIdentifiers =
          GerritUserIdentifiers(Map(authenticatedHttpAuditEvent.who.get -> gerritUserIdentifier)),
        additionalUsersInfo =
          AdditionalUsersInfo(Map(authenticatedHttpAuditEvent.who.get -> additionalUserInfo))
      ).transform(
        auditEventsRDD = sc.parallelize(events),
        timeAggregation = "hour"
      )

    aggregatedEventsDS.collect.length shouldBe 1
    aggregatedEventsDS.collect.head.user_type should contain(userType)
  }

  it should "extract gerrit project from an http event" in {
    val events = Seq(authenticatedHttpAuditEvent)

    val aggregatedEventsDS =
      AuditLogsTransformer(gerritProjects = GerritProjects(Map(project -> GerritProject(project))))
        .transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.collect.length shouldBe 1
    aggregatedEventsDS.collect.head.project should contain(project)
  }

  it should "extract gerrit project from an ssh event" in {
    val events = Seq(sshAuditEvent)

    val aggregatedEventsDS =
      AuditLogsTransformer(gerritProjects = GerritProjects(Map(project -> GerritProject(project))))
        .transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.collect.length shouldBe 1
    aggregatedEventsDS.collect.head.project should contain(project)
  }

  it should "extract sub-command" in {
    val events = Seq(sshAuditEvent.copy(what = "aCommand.aSubCommand"))

    val aggregatedEventsDS =
      AuditLogsTransformer().transform(sc.parallelize(events), timeAggregation = "hour")

    aggregatedEventsDS.collect.length shouldBe 1
    aggregatedEventsDS.collect.head.sub_command should contain("aSubCommand")
  }
}

object AuditLogsTransformerSpec {
  def epochMillisToNearestHour(epochMillis: Long): sql.Timestamp = {
    val millisInOneHour = 3600000
    epochToSqlTimestampOps(epochMillis - (epochMillis % millisInOneHour))
  }
}
