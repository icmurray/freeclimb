name := "project-name"

version := "0.1"

scalaVersion := "2.9.2"

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.0-M3"


libraryDependencies += "play" %% "anorm" % "2.1-09142012" withSources()

libraryDependencies += "postgresql" % "postgresql" % "9.1-901-1.jdbc4"

libraryDependencies += "com.googlecode.flyway" % "flyway-core" % "1.7"

// Test deps
libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"
