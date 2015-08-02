name := """pi_series_backend"""

version := "1.0-SNAPSHOT"

lazy val pi_series_backend = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "0.8.1",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
  "com.typesafe" % "config" % "1.2.1",
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies ++= Seq(
  "org.webjars.bower" % "angular-websocket" % "1.0.13",
  "org.webjars.bower" % "ionic" % "1.0.1"
)