// Copyright (C) 2019 GerritForge Ltd
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

package com.gerritforge.analytics.auditlog.spark.sql.udf

import org.scalatest.{FlatSpec, Matchers}

class SparkExtractorsSpec extends FlatSpec with Matchers {
  behavior of "extractCommand"

  it should "extract gerrit command from gerrit ssh command" in {
    val what = s"gerrit.command.-f.with.some-r.options"
    val accessPath = "SSH_COMMAND"

    SparkExtractors.extractCommand(what, accessPath) shouldBe "gerrit"
  }

  it should "extract replication command from replication ssh command" in {
    val what = s"replication.start.GerritCodeReview/*"
    val accessPath = "SSH_COMMAND"

    SparkExtractors.extractCommand(what, accessPath) shouldBe "replication"
  }

  it should "extract 'LOGIN' command over SSH" in {
    val what = s"LOGIN"
    val accessPath = "SSH_COMMAND"

    SparkExtractors.extractCommand(what, accessPath) shouldBe what
  }

  it should "extract 'LOGOUT' command over SSH" in {
    val what = s"LOGOUT"
    val accessPath = "SSH_COMMAND"

    SparkExtractors.extractCommand(what, accessPath) shouldBe what
  }

  it should "extract GIT 'git-upload-pack' command over SSH" in {
    val what = s"git-upload-pack./spdk/spdk"
    val accessPath = "GIT"

    SparkExtractors.extractCommand(what, accessPath) shouldBe "git-upload-pack"
  }

  it should "extract GIT 'git-receive-pack' command over SSH" in {
    val what = s"git-receive-pack./spdk/spdk"
    val accessPath = "GIT"

    SparkExtractors.extractCommand(what, accessPath) shouldBe "git-receive-pack"
  }

  it should "extract GIT 'git-upload-pack' command over HTTP" in {
    val what = s"https://review.gerrithub.io/rhos-infra/patch-components/git-upload-pack"
    val accessPath = "GIT"

    SparkExtractors.extractCommand(what, accessPath) shouldBe "git-upload-pack"
  }

  it should "extract GIT 'git-receive-pack' command over HTTP" in {
    val what = s"https://review.gerrithub.io/spdk/spdk/info/refs?service=git-receive-pack"
    val accessPath = "GIT"

    SparkExtractors.extractCommand(what, accessPath) shouldBe "git-receive-pack"
  }

  it should "extract 'LOGOUT' command over GIT" in {
    val what = s"LOGOUT"
    val accessPath = "GIT"

    SparkExtractors.extractCommand(what, accessPath) shouldBe what
  }

  it should "extract http method for rest api calls" in {
    val what = s"/config/server/version"
    val accessPath = "REST_API"
    val httpMethod = "GET"

    SparkExtractors.extractCommand(what, accessPath, httpMethod) shouldBe httpMethod
  }

  it should "extract http method for unknown access path" in {
    val what = s"/config/server/version"
    val accessPath = "UNKNOWN"
    val httpMethod = "PUT"

    SparkExtractors.extractCommand(what, accessPath, httpMethod) shouldBe httpMethod
  }

  it should "extract 'what' when access path is unexpected value" in {
    val what = s"any"
    val accessPath = "unexpected"

    SparkExtractors.extractCommand(what, accessPath) shouldBe what
  }

  it should "extract 'what' when access path is JSON_RPC" in {
    val what = s"ProjectAdminService.changeProjectAccess"
    val accessPath = "JSON_RPC"

    SparkExtractors.extractCommand(what, accessPath) shouldBe what
  }

  it should "extract failed SSH authentication when no access path is provided and what is AUTH" in {
    val what = s"AUTH"
    val accessPath = null

    SparkExtractors.extractCommand(what, accessPath) shouldBe SparkExtractors.FAILED_SSH_AUTH
  }

  behavior of "extractCommandArguments"

  it should "extract SSH command arguments" in {
    val sshArguments = "stream-events.-s.patchset-created.-s.change-restored.-s.comment-added"
    val what = s"gerrit.$sshArguments"
    val accessPath = "SSH_COMMAND"

    SparkExtractors.extractCommandArguments(what, accessPath) should contain(sshArguments)
  }

  it should "extract GIT command arguments when in the form git-upload-pack.<gitArguments>" in {
    val gitArguments = "/spdk/spdk.github.io"
    val what = s"git-upload-pack.$gitArguments"
    val accessPath = "GIT"

    SparkExtractors.extractCommandArguments(what, accessPath) should contain(gitArguments)
  }

  it should "extract GIT command arguments when in the form git-receive-pack.<gitArguments>" in {
    val gitArguments = "/spdk/spdk.github.io"
    val what = s"git-receive-pack.$gitArguments"
    val accessPath = "GIT"

    SparkExtractors.extractCommandArguments(what, accessPath) should contain(gitArguments)
  }

  it should "extract GIT commands over HTTP endpoint as-is" in {
    val what = "https://review.gerrithub.io/redhat-openstack/infrared.git/git-upload-pack"
    val accessPath = "GIT"

    SparkExtractors.extractCommandArguments(what, accessPath) should contain(what)
  }

  it should "extract empty arguments for 'LOGOUT' commands" in {
    val what = "LOGOUT"
    val accessPath = "GIT"

    SparkExtractors.extractCommandArguments(what, accessPath) shouldBe empty
  }

  it should "extract REST API endpoint as-is" in {
    val what = "/changes/ffilz%2Fnfs-ganesha~372229/comments"
    val accessPath = "REST_API"

    SparkExtractors.extractCommandArguments(what, accessPath) should contain(what)
  }

  it should "extract empty arguments for a failed ssh authorization" in {
    val what = s"AUTH"
    val accessPath = null

    SparkExtractors.extractCommandArguments(what, accessPath) shouldBe empty
  }

  it should "extract empty arguments a JSON _RPC access path" in {
    val what = s"some_command"
    val accessPath = "JSON_RPC"

    SparkExtractors.extractCommandArguments(what, accessPath) shouldBe empty
  }

  it should "extract empty arguments for an unexpected access path" in {
    val what = s"any"
    val accessPath = "unexpected"

    SparkExtractors.extractCommandArguments(what, accessPath) shouldBe empty
  }
}