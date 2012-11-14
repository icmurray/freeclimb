name := "project-name"

version := "0.1"

scalaVersion := "2.9.2"

// TODO: separate out integration tests to run sequentially
//       allowing unit tests to be run in parallel
parallelExecution in Test := false

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Repository for Spray libraries
resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.github.seratch" %% "scalikesolr" % "(3.6,)" withSources() withJavadoc()

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.0-M4" withSources() withJavadoc()

libraryDependencies += "play" %% "anorm" % "2.1-09142012" withSources() withSources()

libraryDependencies += "postgresql" % "postgresql" % "9.1-901-1.jdbc4" withSources() withJavadoc()

libraryDependencies += "com.googlecode.flyway" % "flyway-core" % "1.7" withSources() withJavadoc()

libraryDependencies += "io.spray" % "spray-can" % "1.0-M5" withSources() withJavadoc()

libraryDependencies += "io.spray" % "spray-routing" % "1.0-M5" withSources() withJavadoc()

libraryDependencies += "io.spray" %%  "spray-json" % "1.2.2" withSources() withJavadoc()

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.3" withSources() withJavadoc()

// required by spray-routing
scalacOptions += "-Ydependent-method-types"

// Test deps
libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test" withSources() withJavadoc()

libraryDependencies += "io.spray" % "spray-testkit" % "1.0-M5" % "test" withSources() withJavadoc()
