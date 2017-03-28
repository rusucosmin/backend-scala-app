/*
 * Project metadata
 */
name := "backendapp"
version := "0.1"
description := "Backend Scala App"
organization := "com.kuende"
organizationHomepage := Some(url("https://github.com/kuende/backend-scala-app"))
resolvers += Resolver.jcenterRepo

/*
 * Compiler
 */
scalaVersion := "2.11.8"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
fork in run := true
fork in Test := true
cancelable in Global := true
test in assembly := {}
mainClass in Compile := Some("com.kuende.backendapp.Application")

lazy val versions = new {
  val finatra        = "2.7.0"
  val finagle        = "6.41.0"
  val logback        = "1.1.7"
  val quill          = "1.1.0"
  val mysqlConnector = "6.0.5"
  val scalatest      = "3.0.0"
  val guice          = "4.0"
  val twitterServer  = "1.26.0"
  val json4s         = "3.5.0"
}

/*
 * Dependencies
 */
libraryDependencies ++= Seq(
  // Configuration
  "com.typesafe" % "config" % "1.3.1",
  // Testing
  "org.scalatest" %% "scalatest" % versions.scalatest % "test",
  // Utility libraries
  "ch.qos.logback" % "logback-classic" % versions.logback,
  // JSON
  "org.json4s" %% "json4s-native"  % versions.json4s,
  "org.json4s" %% "json4s-jackson" % versions.json4s,
  "org.json4s" %% "json4s-ext"     % versions.json4s
)

// database
libraryDependencies ++= Seq(
  // SQL
  "mysql" % "mysql-connector-java" % versions.mysqlConnector,
  // Database
  "io.getquill" %% "quill-sql"           % versions.quill,
  "io.getquill" %% "quill-finagle-mysql" % versions.quill,
  "com.twitter" %% "finagle-mysql"       % versions.finagle,
  // Web server
  "com.twitter" %% "finatra-http"         % versions.finatra,
  "com.twitter" %% "finatra-httpclient"   % versions.finatra,
  "com.twitter" %% "inject-request-scope" % versions.finatra,
  "com.twitter" %% "util-collection"      % versions.finagle
)

// tests
libraryDependencies ++= Seq(
  "com.twitter"                  %% "finatra-http"         % versions.finatra % "test",
  "com.twitter"                  %% "finatra-jackson"      % versions.finatra % "test",
  "com.twitter"                  %% "inject-server"        % versions.finatra % "test",
  "com.twitter"                  %% "inject-app"           % versions.finatra % "test",
  "com.twitter"                  %% "inject-core"          % versions.finatra % "test",
  "com.twitter"                  %% "inject-modules"       % versions.finatra % "test",
  "com.twitter"                  %% "inject-request-scope" % versions.finatra % "test",
  "com.google.inject.extensions" % "guice-testlib"         % versions.guice   % "test",
  "com.twitter"                  %% "finatra-http"         % versions.finatra % "test" classifier "tests",
  "com.twitter"                  %% "finatra-jackson"      % versions.finatra % "test" classifier "tests",
  "com.twitter"                  %% "inject-server"        % versions.finatra % "test" classifier "tests",
  "com.twitter"                  %% "inject-app"           % versions.finatra % "test" classifier "tests",
  "com.twitter"                  %% "inject-core"          % versions.finatra % "test" classifier "tests",
  "com.twitter"                  %% "inject-modules"       % versions.finatra % "test" classifier "tests",
  "org.mockito"                  % "mockito-core"          % "1.9.5"          % "test",
  "org.scalacheck"               %% "scalacheck"           % "1.13.4"         % "test",
  "org.specs2"                   %% "specs2-mock"          % "2.4.17"         % "test"
)

assemblyMergeStrategy in assembly := {
  case s if s.endsWith(".properties")          => MergeStrategy.filterDistinctLines
  case s if s.endsWith("application.conf")     => MergeStrategy.concat
  case s if s.endsWith("pom.xml")              => MergeStrategy.last
  case PathList("google", "protobuf", _ *)     => MergeStrategy.last
  case "BUILD"                                 => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.last
  case "META-INF/mime.types"                   => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

enablePlugins(JavaAppPackaging)

lazy val databaseUrl      = sys.env.getOrElse("MYSQL_URL", "")
lazy val databaseUser     = sys.env.getOrElse("MYSQL_USERNAME", "root")
lazy val databasePassword = sys.env.getOrElse("MYSQL_PASSWORD", "")

flywayLocations := Seq("classpath:db/migration")
flywayUrl := databaseUrl
flywayUser := databaseUser
flywayPassword := databasePassword

/*
 * Code coverage via scoverage
 */
coverageMinimum := 90
coverageFailOnMinimum := true
coverageOutputCobertura := false
coverageOutputHTML := true
coverageOutputXML := false

/*
 * Code formatting
 */
scalafmtConfig := Some(file(".scalafmt.conf"))
