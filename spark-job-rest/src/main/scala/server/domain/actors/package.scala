package server.domain

import akka.util.Timeout
import com.typesafe.config.Config
import org.apache.spark.SparkConf

import scala.concurrent.duration._
import scala.collection.JavaConverters._

/**
 * Utility functions for actors
 */
package object actors {
  implicit val timeout: Timeout = 50 seconds

  def getValueFromConfig[T](config: Config, configPath: String, defaultValue: T): T ={
    if (config.hasPath(configPath)) config.getAnyRef(configPath).asInstanceOf[T] else defaultValue
  }

  def configToSparkConf(config:Config, contextName:String, jars: List[String]): SparkConf ={
    val sparkConf = new SparkConf().setAppName(contextName).setJars(jars)
    for(x <- config.entrySet().asScala if x.getKey.startsWith("spark.")) {
      sparkConf.set(x.getKey, x.getValue.unwrapped().toString)
    }
    sparkConf
  }
}
