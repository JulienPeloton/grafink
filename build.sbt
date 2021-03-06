import BuildHelper._

name := "grafink"

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.astrolabsoftware"
ThisBuild / organizationName := "astrolabsoftware"

scalacOptions ++= Seq("-Ypartial-unification", "-deprecation")

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := scalastyle.in(Compile).toTask("").value
(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value

enablePlugins(BuildInfoPlugin, JavaAppPackaging)

lazy val root =
  (project in file("."))
    .settings(
      // Add support for scala version 2.11
      crossScalaVersions := Seq("2.11.11", (ThisBuild / scalaVersion).value),
      stdSettings
    )

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

// https://github.com/circe/circe/issues/713
// https://stackoverflow.com/questions/43611147/spark-not-working-with-pureconfig
assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("shapeless.**" -> "shadeshapless.@1").inAll,
  ShadeRule.rename("io.netty.**" -> "shadenetty.@1").inAll
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

// removes all jar mappings in universal and appends the fat jar
mappings in Universal := {
  // universalMappings: Seq[(File,String)]
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value

  val filtered = universalMappings filter {
    case (_, name) => !name.endsWith(".jar")
  }

  // add the fat jar to our sequence of things that we've filtered
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

// Put conf files inside conf directory in the package
mappings in Universal ++= {

  ((sourceDirectory in Compile).value / "resources" * "*.conf").get.map { f =>
    f -> s"conf/${f.name}"
  }
}

// the bash scripts classpath only needs the fat jar
scriptClasspath := Seq((assemblyJarName in assembly).value)
// name of start script
bashScriptConfigLocation := Some("")
executableScriptName := "start.sh"
// do not create bat script
makeBatScripts := Seq()

bashScriptExtraDefines += s"""JARNAME=${(assemblyJarName in assembly).value}"""

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

publishMavenStyle := true