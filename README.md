# Active-Learning-DB

## Static
This is the version of things that executes in a standalone, non streaming environment. 

Should be plug and play assuming that your `$SPARKHOME` environment variable is set properly. 
Just clone it, then do the following
```
cd ./static 
sbt package
./run.sh
```

## Streaming
In this version we are streaming from a kafka broker. Currently working on this.

I got something to work, but in order to do it, you need to set up kafka. 
I followed the guide [here](http://www.bogotobogo.com/Hadoop/BigData_hadoop_Zookeeper_Kafka_single_node_single_broker_cluster.php) and [here](http://www.bogotobogo.com/Hadoop/BigData_hadoop_Zookeeper_Kafka.php).
So then I had 1 terminal that ran the following
```
~/kafka/bin/zookeeper-server-start.sh ~/kafka/config/zookeeper.properties
```
another terminal that ran
```
~/kafka/bin/kafka-server-start.sh ~/kafka/config/server.properties
```

Then, in a third terminal, run
```
echo "Hello, Kafka" | ~/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic streamtest
```

Then, in a fourth (and final) terminal

```
cd ./static 
sbt assembly
./run.sh
```

Okay so now that should have a streaming job running. Now, back in your 
third terminal you should be able to keep executing the same line
```
echo "Hello, Kafka" | ~/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic streamtest
```
over and over again, and see corresponding output in the spark terminal!
