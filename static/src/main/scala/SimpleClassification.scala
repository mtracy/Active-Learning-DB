/* SimpleClassification.scala */
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.feature.{HashingTF, Tokenizer, VectorAssembler}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.classification.{LogisticRegressionWithLBFGS, LogisticRegressionModel}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.sql.Row
import org.apache.log4j._
import scala.collection.mutable.ListBuffer
import org.apache.spark.sql.SQLContext
import org.apache.commons.csv.CSVFormat
import org.apache.spark.sql.types.{StructType, StructField, IntegerType, DoubleType};
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics


import ML._

import scala.util.matching.Regex

object SimpleClassification {


    def main(args: Array[String]) {
		Logger.getLogger("org").setLevel(Level.OFF);
		Logger.getLogger("akka").setLevel(Level.OFF);
        val conf = new SparkConf().setAppName("Simple Classification")
        val sc = new SparkContext(conf)
        
        val sqlContext = new org.apache.spark.sql.SQLContext(sc)
        
        import sqlContext.implicits._
        
        val customSchema = StructType(Array(
			StructField("sex", IntegerType, true),
			StructField("age", IntegerType, true),
			StructField("Medu", IntegerType, true),
			StructField("Fedu", IntegerType, true),
			StructField("traveltime", IntegerType, true),
			StructField("studytime", IntegerType, true),
			StructField("failures", IntegerType, true),
			StructField("schoolsup", IntegerType, true),
			StructField("famsup", IntegerType, true),
			StructField("paid", IntegerType, true),
			StructField("activities", IntegerType, true),
			StructField("nursery", IntegerType, true),
			StructField("higher", IntegerType, true),
			StructField("internet", IntegerType, true),
			StructField("romantic", IntegerType, true),
			StructField("famrel", IntegerType, true),
			StructField("freetime", IntegerType, true),
			StructField("goout", IntegerType, true),
			StructField("Dalc", IntegerType, true),
			StructField("Walc", IntegerType, true),
			StructField("health", IntegerType, true),
			StructField("absences", IntegerType, true),
			StructField("G1", IntegerType, true),
			StructField("G2", IntegerType, true),
			StructField("G3", DoubleType, true)))
             
		val df = sqlContext.read
			.format("com.databricks.spark.csv")
			.option("header", "true") // Use first line of all files as header
			.schema(customSchema)
			.load("./src/main/resources/student-mat.csv")
			
		val assembler = new VectorAssembler()
			.setInputCols(Array("sex","age", "Medu", "Fedu",  
								"traveltime", "studytime", "failures", 
								"schoolsup", "famsup", "paid", 
								"activities", "nursery", "higher", 
								"internet", "romantic", "famrel", 
								"freetime", "goout", "Dalc", "Walc", 
								"health", "absences"))
			.setOutputCol("features")
			
		
		val output = assembler.transform(df).withColumnRenamed("G3", "label").select("features", "label").map(row => LabeledPoint(row.getDouble(1), row.getAs[Vector](0)))
		
		val test = output.sample(false, .1, 2) //7 here is a seed for the sampling
		val train = output.subtract(test)

		println(output.count())
		println(test.count())
		println(train.count())
		
		
		
		/*
		var log = new LogReg()
		log.train(train, 2)
		val results = log.testLabeledPoints(test)
		val metrics = new MulticlassMetrics(results)
		val precision = metrics.precision
		println("Precision = " + precision)
		* 
		*/
		
		var svm = new SVM()
		svm.train(train, 1000)
		val results = svm.testLabeledPoints(test)
		results.collect.foreach(println)
		val metrics = new BinaryClassificationMetrics(results)
		val auROC = metrics.areaUnderROC()
		println("Area under ROC = " + auROC)
		
		
		
		
		
		
		
		
		
       
    }
}
