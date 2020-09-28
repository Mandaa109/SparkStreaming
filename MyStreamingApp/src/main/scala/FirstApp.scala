package main.scala
import org.apache.spark.streaming._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import java.io.File
import org.apache.spark.sql.{Row, SaveMode, SparkSession}

object FirstApp {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("HelloSpark").setMaster("local")
    val sc = new SparkContext(conf)
    val x = sc.textFile("abc1.txt")
    val y = x.flatMap(n => n.split(" "))
    val z = y.map(n=>(n,1))
    val result = z.reduceByKey(_+_)
    
    /*
    case class Record(key: Int, value: String)
    val warehouseLocation = new File("spark-warehouse").getAbsolutePath
    
    val spark = SparkSession
    .builder()
    .appName("Spark Hive Example")
    .config("spark.sql.warehouse.dir", warehouseLocation)
    .enableHiveSupport()
    .getOrCreate()
    
    import spark.implicits._
    import spark.sql

    sql("CREATE TABLE IF NOT EXISTS src (key INT, value STRING) USING hive")
    sql(" 'this is data test' " + " INTO TABLE src")
    * 
    */
    
    result.saveAsTextFile("outp" + java.util.UUID.randomUUID.toString)
    sc.stop()
  }  
}