import sbt.Keys.version

enablePlugins(GitVersioning)
enablePlugins(DockerPlugin)

git.useGitDescribe := true

organization := "gerritforge"

name := "analytics-etl"

scalaVersion := "2.11.8"

val sparkVersion = "2.2.1"

val sparkDependencyScope = "provided"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % sparkDependencyScope
    exclude("org.spark-project.spark", "unused"),
  "org.apache.spark" %% "spark-sql" % sparkVersion % sparkDependencyScope,
  "org.elasticsearch" %% "elasticsearch-spark-20" % "5.0.2"
    excludeAll ExclusionRule(organization = "org.apache.spark"),
  // json4s still needed by GerritProjects
  "org.json4s" %% "json4s-native" % "3.2.11",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",

  "com.urswolfer.gerrit.client.rest" % "gerrit-rest-java-client" % "0.8.14",
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "com.github.scopt" %% "scopt" % "3.6.0",
  "org.scalactic" %% "scalactic" % "3.0.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

mainClass in(Compile, run) := Some("com.gerritforge.analytics.job.Main")

parallelExecution in Test := false

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${name.value}-assembly.jar"

  new Dockerfile {
    from("gerritforge/jw2017-spark")
    runRaw("apk add --no-cache wget")
    runRaw("mkdir -p /app")
    add(artifact, artifactTargetPath)
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