package stream
import java.util.HashMap

import kafka.serializer.StringDecoder
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

object kafkatwo {
  implicit val format = DefaultFormats
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("error")
      System.exit(1)
    }
    val Array(brokers, topic) = args
    val sc = new SparkConf().setAppName("two")
    val ssc = new StreamingContext(sc, Seconds(3))
//    val sqls = new SQLContext(sc)
    ssc.checkpoint("two")
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    //    val kafkaParams = Map[String,String]("metadata.broker.list" -> brokers,"auto.offset.reset" -> "smallest")
    val topicset = topic.split(",").toSet
    val lines = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicset).map(_._2)
    val searchEngineinfo = lines.map(_.split("\"")(3)).map(_.split("/")(2)).map(r=>(r,1)).reduceByKey((x,y)=>x+y).filter(x =>(x._2 != 0)).foreachRDD(rdd=>{
//    val word = lines.map(r=>(r,1)).reduceByKey((x,y)=>x+y).filter(x =>(x._2 != 0)).foreachRDD(rdd=>{
////    val word = lines.map(r => (r, 1)).reduceByKeyAndWindow(_ + _, _ - _, Seconds(5), Seconds(3),2).filter(x => (x._2 != 0)).foreachRDD(rdd => {
      if (rdd.count() != 0) {
        val pros = new HashMap[String, Object]()
        pros.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.1.185:9092")
        pros.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        pros.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        val producer = new KafkaProducer[String, String](pros)
        val str = write(rdd.collect())
        //        val st = rdd.collect().mkString(",")
        val massage = new ProducerRecord[String, String]("restule", null, str)


        producer.send(massage)
        println(str)
      }
    })



    ssc.start()
    ssc.awaitTermination()
  }
}

