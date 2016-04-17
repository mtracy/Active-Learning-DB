import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import kafka.serializer.StringDecoder
import org.apache.log4j._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

object StreamingClassification {


    def main(args: Array[String]) {
		val sparkConf = new SparkConf().setAppName("DirectKafkaWordCount")
		val ssc = new StreamingContext(sparkConf, Seconds(2))
		// Create direct kafka stream with brokers and topics
		val topicsSet = Set("streamtest")
		val kafkaParams = Map[String, String]("metadata.broker.list" -> "localhost:9092")
		val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
			ssc, kafkaParams, topicsSet)

    // Get the lines, split them into words, count the words and print
		val lines = messages.map(_._2)
		val words = lines.flatMap(_.split(" "))
		val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
			wordCounts.print()

    // Start the computation
		ssc.start()
		ssc.awaitTermination()

		
    
    }
}
