name := "hm-apiserver"
organization := "com.pwos"
version := "0.1"
scalaVersion := "2.12.8"

lazy val akkaHttpVersion          = "10.1.10"
lazy val akkaVersion              = "2.5.26"
lazy val akkaHttpCirceVersion     = "1.29.1"
lazy val scalaTestVersion         = "3.1.0"
lazy val scalaCheckVersion        = "1.14.2"
lazy val circeVersion             = "0.11.1"
lazy val slickVersion             = "3.3.1"
lazy val slickJodaMapperVersion   = "2.4.0"
lazy val jodaTimeVersion          = "2.7"
lazy val jodaConvertVersion       = "1.7"
lazy val slf4jVersion             = "1.7.12"
lazy val catsVersion              = "1.6.0"
lazy val mySqlConnectorVersion    = "8.0.16"
lazy val pureConfigVersion        = "0.11.1"
lazy val jwtCirceVersion          = "3.0.1"
lazy val bcryptVersion            = "0.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka"       %%  "akka-http"             % akkaHttpVersion,
  "com.typesafe.akka"       %%  "akka-stream"           % akkaVersion,
  "com.typesafe.akka"       %%  "akka-stream-testkit"   % akkaVersion,
  "com.typesafe.akka"       %%  "akka-http-testkit"     % akkaHttpVersion,
  "de.heikoseeberger"       %%  "akka-http-circe"       % akkaHttpCirceVersion,

  "org.typelevel"           %%  "cats-core"             % catsVersion,
  
  "com.typesafe.slick"      %%  "slick"                 % slickVersion,
  "com.typesafe.slick"      %%  "slick-hikaricp"        % slickVersion,
  "mysql"                   %   "mysql-connector-java"  % mySqlConnectorVersion,
  "org.slf4j"               %   "slf4j-simple"          % slf4jVersion,
  "com.github.tototoshi"    %%  "slick-joda-mapper"     % slickJodaMapperVersion,
  "joda-time"               %   "joda-time"             % jodaTimeVersion,
  "org.joda"                %   "joda-convert"          % jodaConvertVersion,

  "com.github.pureconfig"   %%  "pureconfig"            % pureConfigVersion,

  "com.pauldijou"           %%  "jwt-circe"             % jwtCirceVersion,

  "org.mindrot"             %   "jbcrypt"               % bcryptVersion,

  "org.scalacheck"          %%  "scalacheck"            % scalaCheckVersion       % Test, 
  "org.scalatest"           %%  "scalatest"             % scalaTestVersion        % Test
)

libraryDependencies ++= Seq(
  "io.circe"                %%  "circe-core",
  "io.circe"                %%  "circe-generic",
  "io.circe"                %%  "circe-parser"
).map(_ % circeVersion)


def scalacOptionsSeq: Seq[String] = {
  // format: off
  val defaultOpts = Seq(
    "-Ypartial-unification",
    "-deprecation",                     // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",               // Specify character encoding used by source files.
    "-explaintypes",                    // Explain type errors in more detail.
    "-feature",                         // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",           // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",    // Allow macro definition (besides implementation and application)
    "-language:higherKinds",            // Allow higher-kinded types
    "-language:implicitConversions",    // Allow definition of implicit functions called views
    "-unchecked",                       // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                      // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfuture",                         // Turn on future language features.
    "-Xlint:adapted-args",              // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant",                  // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",        // Selecting member of DelayedInit.
    "-Xlint:doc-detached",              // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",              // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                 // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",      // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",              // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",           // Option.apply used implicit view.
    "-Xlint:package-object-classes",    // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",    // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",            // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",               // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",     // A local type parameter shadows a type already in scope.
    "-Ywarn-dead-code",                 // Warn when dead code is identified.
    "-Ywarn-extra-implicit",            // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen",             // Warn when numerics are widened.
    "-Ywarn-unused:implicits",          // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",            // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",             // Warn if a local definition is unused.
    "-Ywarn-unused:params",             // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",            // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",           // Warn if a private member is unused.
    "-Ywarn-value-discard"              // Warn when non-Unit expression results are unused.
  )

  defaultOpts
  // format: on
}

scalacOptions ++= scalacOptionsSeq

fork in Test := true

mainClass in assembly := Some("com.pwos.api.AppLauncher")
test in assembly := {}
