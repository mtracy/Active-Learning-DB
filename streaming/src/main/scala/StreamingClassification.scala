import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import kafka.serializer.StringDecoder
import org.apache.log4j._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf



	
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

    def main(args: Array[String]) {
		Logger.getLogger("org").setLevel(Level.OFF);
		Logger.getLogger("akka").setLevel(Level.OFF);
		val sparkConf = new SparkConf().setAppName("DirectKafkaWordCount")
		val ssc = new StreamingContext(sparkConf, Seconds(2))
		// Create direct kafka stream with brokers and topics
		val topicsSet = Array("MyTopic").toSet
		val kafkaParams = Map[String, String]("zookeeper.connect" -> "192.168.0.101:2181", "metadata.broker.list" -> "192.168.0.101:9092")
		val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
			ssc, kafkaParams, topicsSet)


        
        
        /*
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
			
			
		
		// Get the lines, split them into words, count the words and print
		val lines = messages.map(_._2)

		val s = lines.map{ k => 
			val arr = k.split(",")
			new Student(arr(0).toInt, arr(1).toInt, arr(2).toInt, arr(3).toInt, arr(4).toInt, arr(5).toInt,
					arr(6).toInt, arr(7).toInt, arr(8).toInt, arr(9).toInt, arr(10).toInt,
					arr(11).toInt, arr(12).toInt, arr(13).toInt, arr(14).toInt, arr(15).toInt,
					arr(16).toInt, arr(17).toInt, arr(18).toInt, arr(19).toInt, arr(20).toInt,
					arr(21).toInt, arr(22).toInt, arr(23).toInt, arr(24).toDouble)
		}
		s.foreachRDD{ k =>
			k.foreach{s => s.printPassed()}
		}

		// Start the computation
		ssc.start()
		ssc.awaitTermination()

		
    
    }
}
