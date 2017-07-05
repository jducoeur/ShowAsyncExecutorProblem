name := """show-async-problem"""
organization := "org.querki"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play-slick" % "3.0.0",

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.querki.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.querki.binders._"
