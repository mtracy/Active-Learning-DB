$SPARKHOME/bin/spark-submit --class "SimpleClassification" --packages com.databricks:spark-csv_2.10:1.1.0 --master "local[4]" ./target/scala-2.10/simple-classification_2.10-1.0.jar
