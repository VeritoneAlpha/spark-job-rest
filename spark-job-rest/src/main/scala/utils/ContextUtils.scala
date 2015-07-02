package utils

import com.typesafe.config.Config
import org.apache.spark.SparkConf

import scala.collection.JavaConverters._

object ContextUtils {
  def configToSparkConf(config:Config, contextName:String): SparkConf ={
    val sparkConf = new SparkConf()
      .setAppName(contextName)
      .setJars(config.getStringList("spark.job.rest.context.jars").asScala)

    for(x <- config.entrySet().asScala if x.getKey.startsWith("spark.") && ! x.getKey.startsWith("spark.job.rest.")) {
      sparkConf.set(x.getKey, x.getValue.unwrapped().toString)
    }

    sparkConf
  }
}
