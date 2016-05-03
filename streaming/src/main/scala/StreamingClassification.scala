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
import org.apache.spark.mllib.regression.LinearRegressionModel
import org.apache.spark.mllib.regression.StreamingLinearRegressionWithSGD

import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.dstream.DStream

import org.apache.spark.sql.SQLContext

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import java.util.HashMap




	
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
	
	
	//Takes in a DStream of lines in LIBSVM format and converts it into a DStream of LabeledPoints
	//Takes the number of features expected to parse out bad lines
	//Additionally, takes a parameter that indicates if a label is present in the line (as would be the case in training data
	//Lastly, there is an optional normalizer argument that will normalize the feature vector if present (highly recommended)
	def parse(stream : DStream[String], numFeatures : Int, label : Boolean, normalizer : Normalizer = null) : DStream[LabeledPoint] = {
		var filterval = 0
		if(label == true)
			filterval = numFeatures + 1
		else
			filterval = numFeatures
		
		stream.map{ l =>
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
	
	//This is the feature vector that is used in testing. It assumes there
	//are 9 features that are ambiguously named. The format of the object
	//to be classified could be made configurable.
	case class Tuple(two: Double, three: Double, four: Double, five: Double, six: Double, seven: Double, eight: Double, nine: Double, ten: Double, label: Double)


    def main(args: Array[String]) {
		Logger.getLogger("org").setLevel(Level.OFF);
		Logger.getLogger("akka").setLevel(Level.OFF);
		val sparkConf = new SparkConf().setAppName("Active-Streams")
		val ssc = new StreamingContext(sparkConf, Seconds(2))
		
		/////////////////////////
		//Configurable Settings//
		/////////////////////////
		val zkQuorum = "192.168.0.102:2181"
		val kafkaBroker = "192.168.0.102:9092"
		val group = "test-group"
		val numThreads = "1"
		
		val traintopics = "train"
		val testtopics = "test"
		val weighttopic = "weights"
		
		val normalizer = new Normalizer()
		
		val numFeatures = 9
		val label = true
		val labels = Array(0.0, 1.0)
		val confidenceBound = .25
		val numIterations = 200
		val stepSize = 1
		val updateDelay = 50
		
		val confidentPath = "./data/json/confident.json"
		val unconfidentPath = "./data/json/unconfident.json"
		/////////////////////////
		
		val trainMap = traintopics.split(",").map((_, numThreads.toInt)).toMap
		val train = KafkaUtils.createStream(ssc, zkQuorum, group, trainMap).map(_._2)

		val testMap = testtopics.split(",").map((_, numThreads.toInt)).toMap
		val test = KafkaUtils.createStream(ssc, zkQuorum, group, testMap).map(_._2)
		
		val props = new HashMap[String, Object]()
				props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker)
				props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer")
				props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer")
			
		val producer = new KafkaProducer[String, String](props)
		
		
		val model = new StreamingLinearRegressionWithSGD()
				.setInitialWeights(Vectors.zeros(numFeatures))
				.setNumIterations(numIterations)
				.setStepSize(stepSize)
        
		var trainingCount: Double = 0.0
	
	    val trainLines = parse(train, numFeatures, label, normalizer)  
        model.trainOn(trainLines)
        
			
		
		val testLines = parse(test, numFeatures, label, normalizer)	
		val predictions = model.predictOnValues(testLines.map(lp => (lp.features, lp.features)))
		
		
		
		//filters out the tuples that are not within the confidence bound of a valid label
		//convert these confident tuples into Tuple objects
		//write them to the confident location
		val confident = predictions.filter{ k=>
			val l = k._2
			!(labels.map{ v=> math.abs(l-v)}.filter{v=> v < confidenceBound}.isEmpty)
		}.map { k=>
			k._1.toArray :+ (math.floor(k._2+.5))
		}.map(p => Tuple(p(0), p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8), p(9)))

		confident.foreachRDD { rdd =>
			// Get the singleton instance of SQLContext
			val sqlContext = SQLContext.getOrCreate(rdd.sparkContext)
			import sqlContext.implicits._
	
			// Convert RDD[String] to DataFrame
			val df = rdd.toDF()
			df.write.mode("append").json(confidentPath)
		}
		
		
		
		//filters out the tuples that are within the confidence bound of a valid label
		//convert these unconfident tuples into Tuple objects
		//write them to the unconfident location
		val unconfident = predictions.filter{ k=>
			val l = k._2
			(labels.map{ v=> math.abs(l-v)}.filter{v=> v < confidenceBound}.isEmpty)
		}.map { k=>
			k._1.toArray :+ (math.floor(k._2+.5))
		}.map(p => Tuple(p(0), p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8), p(9)))
		
		unconfident.foreachRDD { rdd =>

			// Get the singleton instance of SQLContext
			val sqlContext = SQLContext.getOrCreate(rdd.sparkContext)
			import sqlContext.implicits._
	
			// Convert RDD[String] to DataFrame
			val df = rdd.toDF()
			df.write.mode("append").json(unconfidentPath)
		}
		
		
		trainLines.foreachRDD{r=>
			
			trainingCount = trainingCount + r.count()
			if(trainingCount >= updateDelay) { 
				
				trainingCount = 0
				
		
				val weights = model.latestModel().weights.toString()
				
				val message = new ProducerRecord[String, String](weighttopic, null, weights)
				
				producer.send(message)
			}
		
		}
		
		ssc.start()
		ssc.awaitTermination()

		
    
    }
}




