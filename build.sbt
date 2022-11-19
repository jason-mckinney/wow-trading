val scala3Version = "3.2.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "wow-trading",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.4.1",
      "co.fs2" %% "fs2-core" % "3.3.0",
      "co.fs2" %% "fs2-io" % "3.3.0",
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "org.typelevel" %% "cats-effect-testing-specs2" % "1.5.0" % Test
    )
  )
