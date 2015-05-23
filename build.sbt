name := "akka-stream-pakkio"
version := "1.0"

scalaVersion := "2.11.6"
val akkaVersion = "1.0-RC3"



libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-stream-experimental_2.10" % "1.0-RC3",
"com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-RC3",
"com.typesafe.akka" % "akka-http-experimental_2.10" % "1.0-RC3",

  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)

    