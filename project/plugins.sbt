addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.4")
addSbtPlugin("org.scoverage"    % "sbt-scoverage"       % "1.4.0")
addSbtPlugin("com.geirsson"     % "sbt-scalafmt"        % "0.4.1")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15-1")

addSbtPlugin("com.github.tkawachi" % "sbt-lock" % "0.3.0")

// Flyway
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.3")
resolvers += "Flyway" at "https://flywaydb.org/repo"
