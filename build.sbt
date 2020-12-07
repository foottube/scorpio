
name := "scorpio"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-feature",
  "-language:_"
)

enablePlugins(JavaAppPackaging)

scriptClasspath +="../conf"

parallelExecution in Test := false

libraryDependencies ++= {
  val akkaVersion = "2.4.12"
  val jettyVersion = "9.3.13.v20161014"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "javax.mail" % "mail" % "1.5.0-b01",
    "org.eclipse.jetty" % "jetty-server" % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion,
    "org.apache.httpcomponents" % "fluent-hc" % "4.5.2",
    "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
    "org.apache.httpcomponents" % "httpasyncclient" % "4.1.2",
    "org.quartz-scheduler" % "quartz" % "2.2.3",
    "org.quartz-scheduler" % "quartz-jobs" % "2.2.3"
  )
}