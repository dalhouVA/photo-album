name := "photoAlbum"

version := "0.1"

scalaVersion := "2.13.1"

val akkaVersion = "2.6.3"
val circeVersion = "0.13.0"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "de.heikoseeberger" %% "akka-http-circe" % "1.31.0",
  "com.typesafe.akka" %% "akka-stream" % "2.6.3",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.4" % Test,
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.11" % Test,
  "de.heikoseeberger" %% "akka-http-circe" % "1.31.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion
)
