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

package com.gerritforge.analytics.gitcommits.model

import org.scalatest.{FlatSpec, Matchers}

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.xml.XML

class GerritEndpointConfigTest extends FlatSpec with Matchers with ManifestXML {

  "gerritProjectsUrl" should "contain prefix when available" in {
    val prefix = "prefixMustBeThere"
    val conf   = GerritEndpointConfig(baseUrl = Some("testBaseUrl"), prefix = Some(prefix))
    conf.gerritProjectsUrl should contain(s"testBaseUrl/projects/?p=$prefix")
  }

  it should "not contain prefix when not available" in {
    val conf = GerritEndpointConfig(baseUrl = Some("testBaseUrl"), prefix = None)
    conf.gerritProjectsUrl should contain(s"testBaseUrl/projects/")
  }

  it should "return projects contained in a repo manifest XML" in {
    val conf = GerritEndpointConfig(baseUrl = Some("testBaseUrl"), manifest = Option(manifestFile.getAbsolutePath))
    val projectNamesFromManifest = conf.projectsFromManifest.toSeq.flatten.map(_.name)

    projectNamesFromManifest should contain only ("sel4_projects_libs", "seL4_tools", "sel4runtime", "musllibc", "seL4_libs", "prefix/util_libs", "sel4test", "nanopb", "opensbi")
    projectNamesFromManifest should not contain ("non-existent-project")
  }

  it should "return projects ids with URL encoded names mentioned in a repo manifest XML" in {
    val conf = GerritEndpointConfig(baseUrl = Some("testBaseUrl"), manifest = Option(manifestFile.getAbsolutePath))
    val projectNamesFromManifest = conf.projectsFromManifest.toSeq.flatten.map(_.id)

    projectNamesFromManifest should contain only ("sel4_projects_libs", "seL4_tools", "sel4runtime", "musllibc", "seL4_libs", "prefix%2Futil_libs", "sel4test", "nanopb", "opensbi")
  }
}

trait ManifestXML {
  // Sample from https://docs.sel4.systems/projects/buildsystem/repo-cheatsheet.html
  lazy val manifestFile: File = {
    val tmpFile = File.createTempFile("analytics-etl", s"-${System.nanoTime()}.xml")
    Files.write(tmpFile.toPath,
      """<manifest>
        |<remote name="seL4" fetch="." />
        |<remote fetch="../sel4proj" name="sel4proj"/>
        |<remote fetch="https://github.com/nanopb" name="nanopb" />
        |<remote fetch="https://github.com/riscv" name="opensbi"/>
        |
        |<default revision="master" remote="seL4" />
        |
        |<project name="seL4_tools.git" path="tools/seL4">
        |    <linkfile src="cmake-tool/init-build.sh" dest="init-build.sh"/>
        |    <linkfile src="cmake-tool/griddle" dest="griddle"/>
        |</project>
        |<project name="sel4runtime.git" path="projects/sel4runtime"/>
        |<project name="musllibc.git" path="projects/musllibc" revision="sel4"/>
        |<project name="seL4_libs.git" path="projects/seL4_libs"/>
        |<project name="prefix/util_libs.git" path="projects/util_libs"/>
        |<project name="sel4test.git" path="projects/sel4test">
        |    <linkfile src="easy-settings.cmake" dest="easy-settings.cmake"/>
        |</project>
        |<project name="sel4_projects_libs" path="projects/sel4_projects_libs" />
        |<project name="opensbi" remote="opensbi" revision="refs/tags/v0.8" path="tools/opensbi"/>
        |<project name="nanopb" path="tools/nanopb" revision="refs/tags/0.4.3" upstream="master" remote="nanopb"/>
        |</manifest>""".stripMargin.getBytes(StandardCharsets.UTF_8))
    tmpFile
  }
}
