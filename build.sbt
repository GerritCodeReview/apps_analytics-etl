import sbt.Keys.version

enablePlugins(GitVersioning)
enablePlugins(DockerPlugin)

git.useGitDescribe := true

organization := "gerritforge"

name := "analytics-etl"

scalaVersion := "2.11.8"

val sparkVersion = "2.1.1"

val gerritApiVersion = "2.13.7"

val pluginName = "analytics-etl"

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
  "com.urswolfer.gerrit.client.rest" % "gerrit-rest-java-client" % "0.8.14",
  "com.github.tomakehurst" % "wiremock" % "1.58" % Test,
  "com.github.scopt" %% "scopt" % "3.6.0",
  "org.scalactic" %% "scalactic" % "3.0.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

mainClass in (Compile,run) := Some("com.gerritforge.analytics.job.Main")

assemblyJarName in assembly := s"${name.value}.jar"

parallelExecution in Test := false

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${name.value}-assembly.jar"

  new Dockerfile {
   from("gerritforge/jw2017-spark")
    runRaw("apk add --no-cache wget")
    runRaw("mkdir -p /app")
    add(artifact, artifactTargetPath )
  }
}

imageNames in docker := Seq(
  ImageName(s"${organization.value}/spark-gerrit-analytics-etl:latest"),

  ImageName(
    namespace = Some(organization.value),
    repository = "spark-gerrit-analytics-etl",
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
