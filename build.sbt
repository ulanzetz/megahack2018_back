import sbt.Keys.libraryDependencies
import sbt.Keys.resolvers

lazy val akkaHttpVersion = "10.1.3"
lazy val circeVersion    = "0.9.3"
lazy val doobieVersion   = "0.5.3"

lazy val appSettings = Seq(
  name := "invalids",
  organization := "org.devrock",
  mainClass in Compile := Some("org.devrock.invalids.EntryPoint"),
  version := "0.1",
  scalaVersion := "2.12.7",
  libraryDependencies ++= Seq(
    "com.typesafe.akka"                  %% "akka-http" % akkaHttpVersion,
    "com.softwaremill.akka-http-session" %% "core"      % "0.5.5"
  ),
  libraryDependencies ++= Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-postgres",
    "org.tpolecat" %% "doobie-hikari"
  ).map(_ % doobieVersion),
  libraryDependencies ++= Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser")
    .map(_ % circeVersion),
  libraryDependencies += "ch.qos.logback"             % "logback-classic" % "1.2.3",
  libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.0",
  libraryDependencies += "com.iheart"                 %% "ficus"          % "1.4.3",
  libraryDependencies += "com.typesafe.akka"          %% "akka-stream"    % "2.5.12",
  dependencyOverrides ++= Seq(
    "org.typelevel"    %% "cats-core"   % "1.2.0",
    "org.typelevel"    %% "cats-effect" % "1.0.0-RC2",
    "org.typelevel"    %% "cats-macros" % "1.2.0",
    "com.typesafe"     % "config"       % "1.3.2",
    "net.java.dev.jna" % "jna"          % "4.5.1",
    "com.chuusai"      %% "shapeless"   % "2.3.3"
  )
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(appSettings)
