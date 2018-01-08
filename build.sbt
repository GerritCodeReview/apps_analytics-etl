name := "GerritAnalytics"

version := "1.0"

scalaVersion := "2.11.8"

val sparkVersion = "2.1.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
    exclude("org.spark-project.spark", "unused"),
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.elasticsearch" %% "elasticsearch-spark-20" % "5.0.2"
    excludeAll ExclusionRule(organization = "org.apache.spark"),
  // json4s still needed by GerritProjects
  "org.json4s" %% "json4s-native" % "3.2.11",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",

  "com.github.scopt" %% "scopt" % "3.6.0",
  "org.scalactic" %% "scalactic" % "3.0.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

mainClass in (Compile,run) := Some("com.gerritforge.analytics.job.Main")

parallelExecution in Test := false