package context

import com.typesafe.config.Config
import org.apache.spark.SparkContext

import scala.util.Try

trait SQLContextFactory extends JobContextFactory {
  /**
   * Creates Spark context from class specified under [[SQLContextFactory.sparkContextFactoryClassNameConfigEntry]]
   * config entry or from [[JobContextFactory.defaultFactoryClassName]]
   * @param config general configuration
   * @param contextName context name
   * @return
   */
  def makeContext(config: Config, contextName: String): C = {
    val sparkContext = getSparkContextFactory(config)
      .makeContext(config: Config, contextName: String)
      .asInstanceOf[SparkContext]
    makeContext(config, sparkContext)
  }

  /**
   * Creates SQL context for specified Spark context.
   * Should be implemented by concrete SQL context factory
   * @param config general configuration
   * @param sparkContext underlying Spark context
   * @return
   */
  def makeContext(config: Config, sparkContext: SparkContext): C

  /**
   * Loads factory for Spark context.
   * @param config general configuration as in [[JobContextFactory.getFactory()]]
   * @return
   */
  def getSparkContextFactory(config: Config): JobContextFactory = {
    val className = Try {
      config.getString(SQLContextFactory.sparkContextFactoryClassNameConfigEntry)
    }.getOrElse(JobContextFactory.defaultFactoryClassName)
    JobContextFactory.getFactory(className)
  }
}

object SQLContextFactory {
  val sparkContextFactoryClassNameConfigEntry = "spark.job.rest.context.spark-context-factory"
}
