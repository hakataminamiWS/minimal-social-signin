name := """minimal-social-signin"""
organization := "com.example"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.6"

libraryDependencies += guice
libraryDependencies += ehcache
libraryDependencies += specs2 % Test

resolvers += Resolver.jcenterRepo
resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "7.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "7.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "7.0.0",
  "com.mohiva" %% "play-silhouette-testkit" % "7.0.0" % "test"
)
// For Typesafe config wrapper
libraryDependencies += "com.iheart" %% "ficus" % "1.5.0"

// For Scala extensions for Google Guice 5.0
libraryDependencies += "net.codingwell" %% "scala-guice" % "5.0.1"

// For Akka 2.6.x and Akka Typed Actors 2.6.x and Scala 2.12.x, 2.13.x
libraryDependencies += "com.enragedginger" %% "akka-quartz-scheduler" % "1.9.1-akka-2.6.x"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"
// TwirlKeys.templateImports := Seq()

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
routesImport += "utils.route.Binders._"
