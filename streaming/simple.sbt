name := "StreamingClassification"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.6.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "1.6.0"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.6.1"
libraryDependencies += "org.apache.kafka" % "kafka_2.10" % "0.8.0"
libraryDependencies += "com.101tec" % "zkclient" % "0.8"
libraryDependencies += "com.yammer.metrics" % "metrics-core" % "2.2.0"



mergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
  case "log4j.properties"                                  => MergeStrategy.discard
  case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
  case "reference.conf"                                    => MergeStrategy.concat
  case _                                                   => MergeStrategy.first
}
