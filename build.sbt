name := "ppade"

version := "0.1"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
	"com.sparkjava" 	% "spark-core" 	% "2.5.5"
)

mainClass in assembly := Some("Main")
