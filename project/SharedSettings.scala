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

import Versions._
import com.typesafe.sbt.GitPlugin.autoImport.git
import sbt.Keys._
import sbt.{Def, ExclusionRule, _}
import sbtassembly.AssemblyKeys
import sbtassembly.AssemblyPlugin.autoImport.{assemblyJarName, _}
import sbtdocker.DockerPlugin.autoImport._

object SharedSettings {
  val elastic4s = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion
  )

  private val dockerRepositoryPrefix = "gerrit-analytics-etl"

  lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.12",
    organization := "gerritforge",
    parallelExecution in Test := false,
    fork in Test := true,
    git.useGitDescribe := true,
    libraryDependencies ++= Seq(
      "org.apache.spark"  %% "spark-core"             % sparkVersion % "provided" exclude ("org.spark-project.spark", "unused"),
      "org.apache.spark"  %% "spark-sql"              % sparkVersion % "provided",
      "org.elasticsearch" %% "elasticsearch-spark-20" % esSpark excludeAll ExclusionRule(
        organization = "org.apache.spark"
      ),
      "org.json4s"                 %% "json4s-native"        % json4s,
      "com.google.gerrit"          % "gerrit-plugin-api"     % gerritApiVersion % Provided withSources (),
      "com.typesafe.scala-logging" %% "scala-logging"        % scalaLogging,
      "com.github.scopt"           %% "scopt"                % scopt,
      "org.scalactic"              %% "scalactic"            % scalactic % "test",
      "org.scalatest"              %% "scalatest"            % scalaTest % "test",
      "com.dimafeng"               %% "testcontainers-scala" % TestContainersScala % Test
    ) ++ elastic4s
  )

  def commonDockerSettings(projectName: String): Seq[Def.Setting[_]] = {
    val repositoryName = Seq(dockerRepositoryPrefix, projectName).mkString("-")
    Seq(
      name := s"analytics-etl-$projectName",
      mainClass in (Compile, run) := Some(s"com.gerritforge.analytics.$projectName.job.Main"),
      packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
        ("Gerrit-ApiType", "plugin"),
        ("Gerrit-PluginName", s"analytics-etl-$projectName"),
        ("Gerrit-Module", s"com.gerritforge.analytics.$projectName.plugin.Module"),
        ("Gerrit-SshModule", s"com.gerritforge.analytics.$projectName.plugin.SshModule"),
        ("Implementation-Title", s"Analytics ETL plugin - $projectName"),
        ("Implementation-URL", "https://gerrit.googlesource.com/plugins/analytics-etl")
      ),
      assemblyJarName in assembly := s"${name.value}.jar",
      docker := (docker dependsOn AssemblyKeys.assembly).value,
      imageNames in docker := Seq(
        ImageName(
          namespace = Some(organization.value),
          repository = repositoryName,
          tag = Some("latest")
        ),
        ImageName(
          namespace = Some(organization.value),
          repository = repositoryName,
          tag = Some(version.value)
        )
      ),
      buildOptions in docker := BuildOptions(cache = false)
    )
  }

  def baseDockerfile(
      projectName: String,
      artifact: File,
      artifactTargetPath: String
  ): Dockerfile = {
    new Dockerfile {
      from("openjdk:8-alpine")
      label("maintainer" -> "GerritForge <info@gerritforge.com>")
      runRaw("apk --update add curl tar bash && rm -rf /var/lib/apt/lists/* && rm /var/cache/apk/*")
      env("SPARK_VERSION", sparkVersion)
      env("SPARK_HOME", "/usr/local/spark-$SPARK_VERSION-bin-hadoop2.7")
      env("PATH", "$PATH:$SPARK_HOME/bin")
      env("SPARK_JAR_PATH", artifactTargetPath)
      env("SPARK_JAR_CLASS", s"com.gerritforge.analytics.$projectName.job.Main")
      runRaw(
        "curl -sL \"http://archive.apache.org/dist/spark/spark-$SPARK_VERSION/spark-$SPARK_VERSION-bin-hadoop2.7.tgz\" | tar -xz -C /usr/local"
      )
      add(artifact, artifactTargetPath)
      runRaw(s"chmod +x $artifactTargetPath")
    }
  }
}

object Versions {
  val Elastic4sVersion    = "6.5.1"
  val sparkVersion        = "2.3.3"
  val gerritApiVersion    = "2.13.7"
  val esSpark             = "7.17.0"
  val scalaLogging        = "3.7.2"
  val scopt               = "3.6.0"
  val scalactic           = "3.0.1"
  val scalaTest           = "3.0.1"
  val json4s              = "3.2.11"
  val TestContainersScala = "0.23.0"
}
