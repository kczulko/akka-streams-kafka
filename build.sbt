name := "akka-streams"

version := "1.0"

import AssemblyKeys._

lazy val commonSettings = Seq(
  scalaVersion := "2.12.1",
  libraryDependencies := Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.4.17",
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "io.kamon" %% "kamon-core" % "0.6.6",
    "io.kamon" %% "kamon-statsd" % "0.6.6",
    "com.typesafe" % "config" % "1.3.1"
  ),
  mainClass in assembly := Some("Main"),
  mergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  },
  dockerfile in docker := {
    val artifact: File = assembly.value
    val destinationPath = s"/root/${artifact.getCanonicalFile.getName}"

    sLog.value.info(s"JAR deployment path: $destinationPath")

    new Dockerfile {
      from("kczulko/mesos-flink-1.2.1")
      add(artifact, destinationPath)
      cmd("java", "-jar", destinationPath)
    }
  },
  buildOptions in docker := BuildOptions(
    cache = false,
    removeIntermediateContainers = BuildOptions.Remove.Always,
    pullBaseImage = BuildOptions.Pull.Always
  )
)

lazy val producer = (project in file("producer"))
    .settings(commonSettings: _*)
    .settings(assemblySettings: _*)
    .settings(imageNames in docker := Seq(ImageName(s"eventmanager/producer:latest")))
    .enablePlugins(DockerPlugin)

lazy val consumer = (project in file("consumer"))
    .settings(commonSettings: _*)
    .settings(assemblySettings: _*)
    .settings(imageNames in docker := Seq(ImageName(s"eventmanager/consumer:latest")))
    .enablePlugins(DockerPlugin)
