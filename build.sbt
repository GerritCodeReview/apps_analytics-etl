import SharedSettings._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtdocker.DockerPlugin.autoImport._

lazy val common = (project in file("common"))
  .settings(commonSettings: _*)
  .settings(assembleArtifact in assembly := false)

lazy val analyticsETLGitCommits = (project in file("gitcommits"))
  .enablePlugins(GitVersioning)
  .enablePlugins(DockerPlugin)
  .settings(commonSettings: _*)
  .settings(commonDockerSettings(projectName = "gitcommits"))
  .settings(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val entryPointBase = s"/app"

      baseDockerfile(projectName = "gitcommits",
                     artifact,
                     artifactTargetPath = s"$entryPointBase/${name.value}-assembly.jar")
        .copy(baseDirectory(_ / "scripts" / "gerrit-analytics-etl-gitcommits.sh").value,
              file(s"$entryPointBase/gerrit-analytics-etl-gitcommits.sh"))
        .copy(baseDirectory(_ / "scripts" / "wait-for-elasticsearch.sh").value,
              file(s"$entryPointBase/wait-for-elasticsearch.sh"))
        .copy(baseDirectory(_ / "scripts" / "wait-for-it.sh").value,
          file(s"$entryPointBase/wait-for-it.sh"))
        .cmd(s"/bin/sh", s"$entryPointBase/gerrit-analytics-etl-gitcommits.sh")
    }
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val analyticsETLAuditLog = (project in file("auditlog"))
  .enablePlugins(GitVersioning)
  .enablePlugins(DockerPlugin)
  .settings(commonSettings: _*)
  .settings(commonDockerSettings(projectName = "auditlog"))
  .settings(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val entryPointBase = s"/app"
      baseDockerfile(projectName = "auditlog",
                     artifact,
                     artifactTargetPath = s"$entryPointBase/${name.value}-assembly.jar")
        .copy(baseDirectory(_ / "scripts" / "gerrit-analytics-etl-auditlog.sh").value,
              file(s"$entryPointBase/gerrit-analytics-etl-auditlog.sh"))
        .copy(baseDirectory(_ / "scripts" / "wait-for-elasticsearch.sh").value,
              file(s"$entryPointBase/wait-for-elasticsearch.sh"))
        .copy(baseDirectory(_ / "scripts" / "wait-for-it.sh").value,
          file(s"$entryPointBase/wait-for-it.sh"))
        .volume(s"$entryPointBase/events/")
        .volume(s"$entryPointBase/data/")
        .cmd(s"/bin/sh", s"$entryPointBase/gerrit-analytics-etl-auditlog.sh")
    }
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val root = (project in file("."))
  .disablePlugins(AssemblyPlugin)
  .settings(test in assembly := {})
  .aggregate(analyticsETLGitCommits, analyticsETLAuditLog)
