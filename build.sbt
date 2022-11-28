import sys.process._
import com.typesafe.sbt.packager.docker._

ThisBuild / scalaVersion := "3.2.1"
ThisBuild / version      := "0.1.0-SNAPSHOT"

val commonApplicationSettings = Seq(
  // https://www.scala-sbt.org/sbt-native-packager/formats/docker.html
  // https://stackoverflow.com/questions/24835831/what-does-sbt-native-packagers-dockerpublishlocal-do
  // to see overall settings run sbt show dockerCommands
  defaultLinuxInstallLocation := "/opt",
  Docker / packageName := name.value.toLowerCase,
  dockerRepository := Some("registry.jmmckinney.net/jmckinney"),
  // dockerExposedPorts := Seq(443),
  dockerBaseImage := "openjdk:18",
  maintainer := "Jason McKinney",
  dockerBuildOptions ++= Seq("--platform", dockerPlatform)
  // NOTE: You must define a main class like so...
  // Compile / mainClass := Some("net.jmmckinney.wowtrading.app.Launch")
)

val useHostDockerPlatform: Boolean = false
val dockerPlatform: String = {
  val platform = if(useHostDockerPlatform) {
    val os: String = "docker version --format '{{.Server.Os}}'" !!
    val arch: String = "docker version --format '{{.Server.Arch}}'" !!

    // Process call above adds a newline and wraps the values in ', so we need to remove that
    def clean(s: String): String = s
      .replaceAll("\n", "")
      .replaceAll("\'", "")

    s"${clean(os)}/${clean(arch)}"
  }
  else "linux/amd64"
  println(s"Using docker platform architecture '$platform'")
  platform
}

lazy val root = (project in file("."))
  .aggregate(core)

lazy val core = (project in file("core"))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "wow-trading",
    commonApplicationSettings,
    Compile / mainClass := Some("net.jmmckinney.wowtrading.app.Launch"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.4.1",
      "co.fs2" %% "fs2-core" % "3.3.0",
      "co.fs2" %% "fs2-io" % "3.3.0",
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "com.typesafe" % "config" % "1.4.2",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.19.0",
      "org.apache.logging.log4j" % "log4j-api"        % "2.19.0",
      "org.apache.logging.log4j" % "log4j-core"       % "2.19.0",
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-hikari"    % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-postgres-circe" % "1.0.0-RC2",
      "com.softwaremill.sttp.client3" %% "core" % "3.8.3",
      "com.softwaremill.sttp.client3" %% "fs2" % "3.8.3",
      "com.ocadotechnology" %% "sttp-oauth2" % "0.15.2",
      "com.ocadotechnology" %% "sttp-oauth2-cache-cats" % "0.15.2",
      "org.typelevel" %% "cats-effect-testing-specs2" % "1.5.0" % Test
    )
  )