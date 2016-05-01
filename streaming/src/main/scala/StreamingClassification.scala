import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import kafka.serializer.StringDecoder
import org.apache.log4j._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.StreamingLinearRegressionWithSGD

import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.dstream.DStream

import org.apache.spark.sql.SQLContext



	
object StreamingClassification {	
	
	
	class Student(
	val sex: Int,
	val age: Int,
	val Medu: Int,
	val Fedu: Int,
	val travel: Int,
	val study: Int,
	val failures: Int,
	val schoolsup: Int,
	val famsup: Int,
	val paid: Int,
	val activities: Int,
	val nursery: Int,
	val higher: Int,
	val internet: Int,
	val romantic: Int,
	val famrel: Int,
	val freetime: Int,
	val goout: Int,
	val Dalc: Int,
	val Walc: Int,
	val health: Int,
	val absences: Int,
	val G1: Int,
	val G2: Int,
	val G3: Double){
		
		def printPassed() = {
			println(G3)
		}
		
	}
	
	def parse(stream : InputDStream[scala.Tuple2[String,String]], numFeatures : Int, label : Boolean, normalizer : Normalizer = null) : DStream[LabeledPoint] = {
		var filterval = 0
		if(label == true)
			filterval = numFeatures + 1
		else
			filterval = numFeatures
		
		stream.map(_._2).map{ l =>
			l.split(" +")
		}.filter{ arr =>
			arr.length == filterval			
		}.map { arr =>
			val label = arr(0).toDouble
			val t = arr.slice(1, arr.length).map{ v=>
				(v.split(':'))
			}
			val indices = t.map{v => v(0).toInt}
			val values = t.map{v => v(1).toDouble}
			if (normalizer != null)
				LabeledPoint(label, normalizer.transform(Vectors.dense(values)))
			else
				LabeledPoint(label, Vectors.dense(values))
		}
	}
	
	
	case class Tuple(two: Double, three: Double, four: Double, five: Double, six: Double, seven: Double, eight: Double, nine: Double, ten: Double, label: Double)


    def main(args: Array[String]) {
		Logger.getLogger("org").setLevel(Level.OFF);
		Logger.getLogger("akka").setLevel(Level.OFF);
		val sparkConf = new SparkConf().setAppName("DirectKafkaWordCount")
		val ssc = new StreamingContext(sparkConf, Seconds(2))
		
		// Create direct kafka stream with brokers and topics
		val trainset = Set("labeled")
		val testset = Set("unlabeled")
		val kafkaParams = Map[String, String]("zookeeper.connect" -> "192.168.0.102:2181", "metadata.broker.list" -> "192.168.0.102:9092", "group.id" -> "workers")
		val trainstream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
			ssc, kafkaParams, trainset)
		val teststream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
			ssc, kafkaParams, testset)

		val normalizer = new Normalizer()
		
		val numFeatures = 9
		
		val labels = Array(0.0, 1.0)
		val confidenceBound = .25
		
        
        

			
		
		
		
		val lines = parse(trainstream, numFeatures, true, normalizer).cache()		
		
		
		val model = new StreamingLinearRegressionWithSGD()
			.setInitialWeights(Vectors.zeros(numFeatures))
			.setNumIterations(200)
			.setStepSize(1)
		model.trainOn(lines)
		
		
		
		val test = parse(teststream, numFeatures, true, normalizer)
		
		
		
		val predictions = model.predictOnValues(test.map(lp => (lp.features, lp.features)))
		
		
		val confident = predictions.filter{ k=>
			val l = k._2
			!(labels.map{ v=> math.abs(l-v)}.filter{v=> v < confidenceBound}.isEmpty)//mmm thats hot. scala you are sexy
		}.map { k=>
			k._1.toArray :+ (math.floor(k._2+.5))
		}.map(p => Tuple(p(0), p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8), p(9)))
		


		confident.foreachRDD { rdd =>

			// Get the singleton instance of SQLContext
			val sqlContext = SQLContext.getOrCreate(rdd.sparkContext)
			import sqlContext.implicits._
	
			// Convert RDD[String] to DataFrame
			val df = rdd.toDF()
			df.write.mode("append").json("./data/json/confident.json")
		}
		
		
		/*confident.foreachRDD{r=>
			r.foreach{k=> println(k.mkString(" "))}
		}*/
		
		
		
		
		
		/*
		 * TODO
		 * Write confidently labeled tuples to a file
		 * Write unconfident tuples to somewhere else
		 * 
		 */

		// Start the computation
		ssc.start()
		ssc.awaitTermination()

		
    
    }
}




        /* I'm tired of looking at this but I don't want to throw it away yet
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
		*/
