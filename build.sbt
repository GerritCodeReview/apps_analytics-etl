import sbt.Keys.version
import sbtassembly.AssemblyKeys

enablePlugins(GitVersioning)
enablePlugins(DockerPlugin)

git.useGitDescribe := true

organization := "gerritforge"

name := "analytics-etl"

scalaVersion := "2.11.8"

val sparkVersion = "2.3.2"

val gerritApiVersion = "2.13.7"

val pluginName = "analytics-etl"

val mainClassPackage = "com.gerritforge.analytics.job.Main"
val dockerRepository = "spark-gerrit-analytics-etl"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
  exclude("org.spark-project.spark", "unused"),
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.elasticsearch" %% "elasticsearch-spark-20" % "6.2.0"
  excludeAll ExclusionRule(organization = "org.apache.spark"),
  // json4s still needed by GerritProjects
  "org.json4s" %% "json4s-native" % "3.2.11",
  "com.google.gerrit" % "gerrit-plugin-api" % gerritApiVersion % Provided withSources(),

  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",

  "com.github.scopt" %% "scopt" % "3.6.0",
  "org.scalactic" %% "scalactic" % "3.0.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

mainClass in (Compile,run) := Some(mainClassPackage)

assemblyJarName in assembly := s"${name.value}.jar"

parallelExecution in Test := false

// Docker settings
docker := (docker dependsOn AssemblyKeys.assembly).value

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${name.value}-assembly.jar"
  val entryPointPath = s"/app/gerrit-analytics-etl.sh"

  new Dockerfile {
    from("openjdk:8-alpine")
    label("maintainer" -> "GerritForge <info@gerritforge.com>")
    runRaw("apk --update add curl tar bash && rm -rf /var/lib/apt/lists/* && rm /var/cache/apk/*")
    env("SPARK_VERSION", sparkVersion)
    env("SPARK_HOME", "/usr/local/spark-$SPARK_VERSION-bin-hadoop2.7")
    env("PATH","$PATH:$SPARK_HOME/bin")
    env("SPARK_JAR_PATH", artifactTargetPath)
    env("SPARK_JAR_CLASS",mainClassPackage)
    runRaw("curl -sL \"http://www-eu.apache.org/dist/spark/spark-$SPARK_VERSION/spark-$SPARK_VERSION-bin-hadoop2.7.tgz\" | tar -xz -C /usr/local")
    copy(baseDirectory(_ / "scripts" / "gerrit-analytics-etl.sh").value, file(entryPointPath))
    add(artifact, artifactTargetPath)
    runRaw(s"chmod +x $artifactTargetPath")
    cmd(s"/bin/sh", entryPointPath)
  }
}
imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = dockerRepository,
    tag = Some("latest")
  ),

  ImageName(
    namespace = Some(organization.value),
    repository = dockerRepository,
    tag = Some(version.value)
  )
)
buildOptions in docker := BuildOptions(cache = false)

packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
  ("Gerrit-ApiType", "plugin"),
  ("Gerrit-PluginName", pluginName),
  ("Gerrit-Module", "com.gerritforge.analytics.plugin.Module"),
  ("Gerrit-SshModule", "com.gerritforge.analytics.plugin.SshModule"),
  ("Implementation-Title", "Analytics ETL plugin"),
  ("Implementation-URL", "https://gerrit.googlesource.com/plugins/analytics-etl")
)
