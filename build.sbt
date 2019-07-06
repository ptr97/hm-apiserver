name := "hm-apiserver"
organization := "com.pwos"
version := "0.1"
scalaVersion := "2.12.8"

lazy val akkaHttpVersion  = "10.1.8"
lazy val akkaVersion      = "2.5.22"
lazy val scalaTestVersion = "3.0.8"
lazy val circeVersion     = "0.11.1"
lazy val slickVersion     = "3.3.1"
lazy val catsVersion      = "1.6.0"


scalacOptions += "-Ypartial-unification"


libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka"   %% "akka-stream"          % akkaVersion,

  "org.typelevel"       %% "cats-core"            % catsVersion,

  "com.typesafe.slick"  %% "slick"               % slickVersion,
  "org.slf4j"           %  "slf4j-nop"           % "1.7.26",
  "com.typesafe.slick"  %% "slick-hikaricp"      % slickVersion,
)

libraryDependencies ++= Seq(
  "io.circe"            %% "circe-core",
  "io.circe"            %% "circe-generic",
  "io.circe"            %% "circe-parser"
).map(_ % circeVersion)
