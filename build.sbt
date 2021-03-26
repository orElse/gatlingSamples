import sbt._
import sbt.Keys._

lazy val GatlingVersion = "3.4.2"
val AkkaVersion = "2.6.11"
val AkkaHttpVersion = "10.2.4"

ThisBuild / scalaVersion := "2.12.11"

lazy val hello = (project in file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    name := "gatlingSamples",
    // necessary for test server only
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
    ),
    // the actual dependencies for gatling
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % GatlingVersion % "test",
      "io.gatling" % "gatling-test-framework" % GatlingVersion % "test"
    )
  )

