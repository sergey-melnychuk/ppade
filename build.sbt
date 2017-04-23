name := "ppade"

version := "0.1"

scalaVersion := "2.11.8"

lazy val akkaVersion = "2.4.2"

lazy val akkaHttpVersion = "10.0.5"

libraryDependencies ++= Seq(
	"com.sparkjava" 	% "spark-core" 	% "2.5.5",

	"com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

	"com.typesafe.akka" %% "akka-actor" % akkaVersion,
	"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
	"org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

mainClass in assembly := Some("Main")
