package server.domain

import akka.util.Timeout
import com.typesafe.config.Config
import org.apache.spark.SparkConf

import scala.collection.JavaConverters._

/**
 * Created by raduc on 13/11/14.
 */
package object actors {
  implicit val timeout = Timeout(50000)

  def getValueFromConfig[T](config: Config, configPath: String, defaultValue: T): T ={
    return if (config.hasPath(configPath)) config.getAnyRef(configPath).asInstanceOf[T] else defaultValue
  }

  def configToSparkConf(config:Config, contextName:String, jars: Array[String]): SparkConf ={
    val sparkConf = new SparkConf().setAppName(contextName).setJars(jars)
    for(x <- config.entrySet().asScala
      if(x.getKey.startsWith("spark."))){
      sparkConf.set(x.getKey, x.getValue.unwrapped().toString)
    }
    sparkConf
  }
}
