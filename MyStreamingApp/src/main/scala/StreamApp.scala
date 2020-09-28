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
  
  case class Record(key: Int, value: String)
    
    /*
    val conf = new SparkConf
      .set("spark.sql.warehouse.dir", "hdfs://namenode/sql/metadata/hive")
      .set("spark.sql.catalogImplementation","hive")
      .setMaster("local[*]")
      .setAppName("Hive Example")
      * 
      */
    
    val spark = SparkSession
    .builder()
    .appName("Spark Hive Example")
    .config("spark.sql.warehouse.dir", warehouseLocation)
    .enableHiveSupport()
    .getOrCreate()
    
    import spark.implicits._
    import spark.sql

    sql("CREATE TABLE IF NOT EXISTS src (key INT, value STRING) USING hive")
    sql("LOAD DATA LOCAL INPATH 'C:/Users/manda/eclipse-workspace/MyStreamingApp/spark-warehouse/examples/kv1.txt' INTO TABLE src")
    //sql(" 'this is data test' " + " INTO TABLE src")
    
    // Queries are expressed in HiveQL
sql("SELECT * FROM src").show()
// +---+-------+
// |key|  value|
// +---+-------+
// |238|val_238|
// | 86| val_86|
// |311|val_311|
// ...

// Aggregation queries are also supported.
sql("SELECT COUNT(*) FROM src").show()
// +--------+
// |count(1)|
// +--------+
// |    500 |
// +--------+

// The results of SQL queries are themselves DataFrames and support all normal functions.
val sqlDF = sql("SELECT key, value FROM src WHERE key < 10 ORDER BY key")

// The items in DataFrames are of type Row, which allows you to access each column by ordinal.
val stringsDS = sqlDF.map {
  case Row(key: Int, value: String) => s"Key: $key, Value: $value"
}
stringsDS.show()
// +--------------------+
// |               value|
// +--------------------+
// |Key: 0, Value: val_0|
// |Key: 0, Value: val_0|
// |Key: 0, Value: val_0|
// ...

// You can also use DataFrames to create temporary views within a SparkSession.
val recordsDF = spark.createDataFrame((1 to 100).map(i => Record(i, s"val_$i")))
recordsDF.createOrReplaceTempView("records")

// Queries can then join DataFrame data with data stored in Hive.
sql("SELECT * FROM records r JOIN src s ON r.key = s.key").show()
// +---+------+---+------+
// |key| value|key| value|
// +---+------+---+------+
// |  2| val_2|  2| val_2|
// |  4| val_4|  4| val_4|
// |  5| val_5|  5| val_5|
// ...

// Create a Hive managed Parquet table, with HQL syntax instead of the Spark SQL native syntax
// `USING hive`
sql("CREATE TABLE hive_records(key int, value string) STORED AS PARQUET")
// Save DataFrame to the Hive managed table
val df = spark.table("src")
df.write.mode(SaveMode.Overwrite).saveAsTable("hive_records")
// After insertion, the Hive managed table has data now
sql("SELECT * FROM hive_records").show()
// +---+-------+
// |key|  value|
// +---+-------+
// |238|val_238|
// | 86| val_86|
// |311|val_311|
// ...

// Prepare a Parquet data directory
val dataDir = "/tmp/parquet_data"
spark.range(10).write.parquet(dataDir)
// Create a Hive external Parquet table
sql(s"CREATE EXTERNAL TABLE hive_bigints(id bigint) STORED AS PARQUET LOCATION '$dataDir'")
// The Hive external table should already have data
sql("SELECT * FROM hive_bigints").show()
// +---+
// | id|
// +---+
// |  0|
// |  1|
// |  2|
// ... Order may vary, as spark processes the partitions in parallel.

// Turn on flag for Hive Dynamic Partitioning
spark.sqlContext.setConf("hive.exec.dynamic.partition", "true")
spark.sqlContext.setConf("hive.exec.dynamic.partition.mode", "nonstrict")
// Create a Hive partitioned table using DataFrame API
df.write.partitionBy("key").format("hive").saveAsTable("hive_part_tbl")
// Partitioned column `key` will be moved to the end of the schema.
sql("SELECT * FROM hive_part_tbl").show()
// +-------+---+
// |  value|key|
// +-------+---+
// |val_238|238|
// | val_86| 86|
// |val_311|311|
// ...

  spark.stop()
  //result.saveAsTextFile("outp" + java.util.UUID.randomUUID.toString)
  wordCounts.print()
  ssc.start()
  ssc.awaitTermination()
}