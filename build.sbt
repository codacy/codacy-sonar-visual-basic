import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

organization := "codacy"

name := "codacy-sonar-csharp"

version := "1.0.0-SNAPSHOT"

val languageVersion = "2.12.7"

scalaVersion := languageVersion

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.7.3",
  "com.codacy" %% "codacy-engine-scala-seed" % "3.0.9" withSources(),
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.google.protobuf" % "protobuf-java" % "3.2.0",
  "com.github.tkqubo" % "html-to-markdown" % "0.3.0"
)

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

version in Docker := "1.0"

val sonarVersion = {
  IO.readLines(new File("./.SONAR_VERSION")).mkString.trim
}

val installAll =
 s"""export SONAR_VERSION="$sonarVersion" &&
    |export SONAR_BINS="/opt/docker/sonar-csharp" &&
    |rm -rf "$${SONAR_BINS}" &&
    |mkdir -p "$${SONAR_BINS}" &&
    |apk update &&
    |apk add bash wget unzip &&
    |wget --no-check-certificate -O /tmp/sonar-csharp-plugin-"$${SONAR_VERSION}".jar https://oss.sonatype.org/content/repositories/releases/org/sonarsource/dotnet/sonar-csharp-plugin/"$${SONAR_VERSION}"/sonar-csharp-plugin-"$${SONAR_VERSION}".jar &&
    |unzip /tmp/sonar-csharp-plugin-"$${SONAR_VERSION}".jar -d /tmp/sonar-plugin &&
    |unzip /tmp/sonar-plugin/SonarAnalyzer.Scanner.zip -d "$${SONAR_BINS}" &&
    |rm -rf /tmp/sonar-plugin /tmp/sonar-csharp-plugin-"$${SONAR_VERSION}".jar &&
    |rm -rf /tmp/* &&
    |rm -rf /var/cache/apk/*""".stripMargin.replaceAll(System.lineSeparator(), " ")

mappings.in(Universal) ++= resourceDirectory
  .in(Compile)
  .map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    (for {
      path <- better.files.File(src.toPath).listRecursively()
      if !path.isDirectory
    } yield path.toJava -> path.toString.replaceFirst(src.toString, dest)).toSeq
  }
  .value


val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "codacy/codacy-sonar-csharp-base:latest"

mainClass in Compile := Some("com.codacy.dotnet.Engine")

dockerCommands := dockerCommands.value.flatMap {
  case cmd@(Cmd("ADD", _)) => List(
    Cmd("RUN", s"adduser -u 2004 -D $dockerUser"),
    cmd,
    Cmd("RUN", installAll),
    Cmd("RUN", "mv /opt/docker/docs /docs"),
    ExecCmd("RUN", Seq("chown", "-R", s"$dockerUser:$dockerGroup", "/docs"): _*)
  )
  case other => List(other)
}
