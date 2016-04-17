name := "Simple Classification"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.6.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "1.6.0"
libraryDependencies += "com.databricks" % "spark-csv_2.10" % "1.0.0"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.6.1"

