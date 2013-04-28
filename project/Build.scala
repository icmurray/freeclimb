import sbt._
import Keys._

object Freeclimbers extends Build {

  lazy val globalSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion in ThisBuild := "2.10.1",
    organization in ThisBuild := "org.freeclimbers",
    javaOptions in ThisBuild  ++= Seq("-Xmx2G"),
    scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")
  )

  lazy val root = Project(id        = "freeclimbers",
                          base      = file("."),
                          aggregate = Seq(core, api))

  lazy val core = Project(id        = "freeclimbers-core",
                          base      = file("core"),
                          settings  = globalSettings)

  lazy val api  = Project(id        = "freeclimbers-api",
                          base      = file("api"),
                          settings  = globalSettings ++ Seq(

                            organization := "org.freeclimbers.api",

                            resolvers += "spray repo" at "http://repo.spray.io",
                            resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",

                            libraryDependencies ++= Seq(
                              "com.typesafe.akka"         %% "akka-actor"                    % "2.1.2",
                              "io.spray"                   % "spray-can"                     % "1.1-M7",
                              "io.spray"                   % "spray-routing"                 % "1.1-M7",
                              "io.spray"                  %% "spray-json"                    % "1.2.3",
                              "org.scalaz"                %% "scalaz-core"                   % "7.0.0-M9",
                              "org.scalatest"             %% "scalatest"                     % "1.9.1"        % "test",
                              "org.scalamock"             %% "scalamock-scalatest-support"   % "3.0.1"        % "test",
                              "io.spray"                   % "spray-testkit"                 % "1.1-M7"       % "test"

                            )
                          )
                  ) dependsOn(core)
}
