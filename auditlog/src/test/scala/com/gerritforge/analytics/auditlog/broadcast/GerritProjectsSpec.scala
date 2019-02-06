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

package com.gerritforge.analytics.auditlog.broadcast
import java.net.URLEncoder

import org.scalatest.{FlatSpec, Matchers}

class GerritProjectsSpec extends FlatSpec with Matchers {
  behavior of "extract project from SSH commands"

  it should "extract nothing where project could not be found in existing projects" in {
    val project = "spdk/spdk.github.io"
    val what = s"gerrit.query.--format.json.--current-patch-set.project:$project"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) shouldBe empty
  }

  it should "extract nothing where command does not contain project" in {
    val what = s"gerrit.command.not.targeting.any.project"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) shouldBe empty
  }

  it should "extract nothing where command is LOGIN" in {
    val what = s"LOGIN"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) shouldBe empty
  }

  it should "extract a project from a gerrit query command, where the project is delimited by a space" in {
    val project = "spdk/spdk.github.io"
    val what = s"gerrit.query.--format.json.--current-patch-set.project:$project commit:7f4e8237114e957e727707ab6549d482d40d7c92 NOT is:draft"
    val accessPath = "SSH_COMMAND"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project from a gerrit query command, where the project is wrapped by curly brackets" in {
    val project = "dalihub/dali-scene-template"
    val what = s"gerrit.query.project:{^$project}.--format=JSON.--all-approvals.--comments.--all-reviewers"
    val accessPath = "SSH_COMMAND"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project from a gerrit query command, where the project is dot '.' joined with its arguments" in {
    val project = "redhat-performance/quads"
    val what = s"gerrit.query.--format=JSON.project:$project.status:open"
    val accessPath = "SSH_COMMAND"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project from a gerrit index command" in {
    val project = "Accumbo/iOS"
    val what = s"gerrit.index.project.$project"
    val accessPath = "SSH_COMMAND"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project included in parenthesis" in {

    val specificProject = s"spdk/spdk.gerrithub.io"

    val what = s"""gerrit.query.--format.json.--current-patch-set.(project:$specificProject) commit:"d2da91b1878f7b7ccd3f30f766ffa2f35cc2011d" NOT is:draft"""
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) should contain(specificProject)
  }

  it should "extract the most specific project when multiple projects names are substrings of each other" in {
    val genericProject = "redhat-performance/quads"

    val specificProject = s"$genericProject.github.com"

    val what = s"gerrit.query.--format=JSON.project:$specificProject.status:open"
    val accessPath = "SSH_COMMAND"

    val existingProjects = GerritProjects(Map(
      genericProject -> GerritProject(genericProject),
      specificProject -> GerritProject(specificProject)
    ))

    existingProjects.extractProject(what, accessPath) should contain(specificProject)
  }

  it should "extract a project from a receive pack command" in {
    val project = "att-comdev/prometheus-openstack-exporter"
    val what = s"git-receive-pack./$project"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project from an upload pack command" in {
    val project = "dalihub/dali-toolkit"
    val what = s"git-upload-pack./$project"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project from a replication start command that has some arguments" in {
    val project = "singh4java/springboot"
    val what = s"replication.start.$project.--wait.--now"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project from a replication start command that has no arguments" in {
    val project = "singh4java/springboot.github.com"
    val what = s"replication.start.$project"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) should contain(project)
  }

  it should "extract nothing when replication start refers to no project" in {
    val what = s"replication.start.--help"
    val accessPath = "SSH_COMMAND"

    GerritProjects.empty.extractProject(what, accessPath) shouldBe empty
  }

  it should "extract a project when is found in at the end of the command, even though the word 'project' is not part of the command" in {
    val project = "Sonos-Inc/old97s"
    val what = s"gerrit.query.--format=JSON.--current-patch-set.$project"
    val accessPath = "SSH_COMMAND"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  behavior of "REST API calls"

  it should "extract the decoded project name from a /changes/ url" in {
    val project = "true-wealth/b2b"
    val what = s"/changes/${URLEncoder.encode(project, "UTF-8")}~425967/revisions/5/actions"
    val accessPath = "REST_API"

    GerritProjects.empty.extractProject(what, accessPath) should contain(project)
  }

  it should "extract the decoded project name from a /projects/ url" in {
    val project = "VidScale/UDNTests"
    val what = s"/projects/${URLEncoder.encode(project, "UTF-8")}"
    val accessPath = "REST_API"

    GerritProjects.empty.extractProject(what, accessPath) should contain(project)
  }

  behavior of "GIT HTTP calls"

  it should "extract the project from an info ref upload pack target" in {
    val project = "dushyantRathore/Jenkins_Test"
    val what = s"https://review.gerrithub.io/$project/info/refs?service=git-upload-pack"
    val accessPath = "GIT"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract a project from an SSH upload pack command" in {
    val project = "cbdr/Gjallarhorn"
    val what = s"git-upload-pack./$project"
    val accessPath = "GIT"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract the project from an info ref receive pack target" in {
    val project = "dushyantRathore/Jenkins_Test"
    val what = s"https://review.gerrithub.io/$project/info/refs?service=git-receive-pack"
    val accessPath = "GIT"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract the project from an upload pack target" in {
    val project = "att-comdev/cicd"
    val what = s"https://review.gerrithub.io/$project/git-upload-pack"
    val accessPath = "GIT"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

  it should "extract the project from a receive pack target" in {
    val project = "att-comdev/cicd"
    val what = s"https://review.gerrithub.io/$project/git-receive-pack"
    val accessPath = "GIT"

    val existingProjects = GerritProjects(Map(
      project -> GerritProject(project)
    ))

    existingProjects.extractProject(what, accessPath) should contain(project)
  }

}
