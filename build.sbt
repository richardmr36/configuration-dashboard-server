name := """configuration-dashboard-server"""
organization := "com.myprograms"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.4"

libraryDependencies += guice
libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % "3.8.1",
  "com.google.code.gson" % "gson" % "2.2.4",
  ehcache
)
