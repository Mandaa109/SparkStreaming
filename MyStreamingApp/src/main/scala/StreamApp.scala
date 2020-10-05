package main.scala
import org.apache.spark.streaming._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import java.io.File
import org.apache.spark.sql.{Row, SaveMode, SparkSession}


object StreamApp extends App{
  val warehouseLocation = new File("spark-warehouse").getAbsolutePath
  
  val conf = new SparkConf().setAppName("SparkStreaming").setMaster("local[2]")
      .set("spark.sql.warehouse.dir", warehouseLocation)
      .set("spark.sql.catalogImplementation","hive")
  val sc = new SparkContext(conf);
  val ssc = new StreamingContext(sc, Seconds(20))
  val streamRDD = ssc.socketTextStream("127.0.0.1", 2222)
  val wordCounts = streamRDD.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey(_+_)
  //wordCounts.saveAsTextFile(outputFile)
 // wordCounts.saveAsTextFiles("C:/Users/manda/eclipse-workspace/MyStreamingApp/spark-warehouse/examples/kv1","txt")

  
  wordCounts.foreachRDD(t=> {
         val test = t
         test.saveAsTextFile("C:/Users/manda/eclipse-workspace/MyStreamingApp/spark-warehouse/examples/kv1")
})



  case class Record(key: Int, value: String)
    
    val spark = SparkSession
    .builder()
    .appName("Spark Hive Example")
    .config("spark.sql.warehouse.dir", warehouseLocation)
    .enableHiveSupport()
    .getOrCreate()
    
    import spark.implicits._
    import spark.sql

    sql("CREATE TABLE IF NOT EXISTS src (key INT, value STRING) USING hive")
    sql("LOAD DATA LOCAL INPATH 'C:/Users/manda/eclipse-workspace/MyStreamingApp/spark-warehouse/examples/kv1/part-00000' INTO TABLE src")
    //sql(" 'this is data test' " + " INTO TABLE src")
    
    // Queries are expressed in HiveQL
    sql("SELECT * FROM src").show()


    // Aggregation queries are also supported.
    sql("SELECT COUNT(*) FROM src").show()

  
    wordCounts.print()
    ssc.start()
    ssc.awaitTermination()
}