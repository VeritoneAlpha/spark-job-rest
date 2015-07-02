package context

import api.ContextLike
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import server.domain.actors.getValueFromConfig

trait JobContextFactory {
  type C <: ContextLike
  def makeContext(config: Config, contextName: String): C
}

object JobContextFactory {
  val logger = LoggerFactory.getLogger(getClass)
  val defaultFactoryClassName = "context.SparkContextFactory"
  val classNameConfigEntry = "spark.job.rest.context.job-context-factory"

  /**
   * Loads context factory with a class name in `context.factory`
   * @param config config for context and context factory
   * @param contextName context name
   * @return Spark context
   */
  def makeContext(config: Config, contextName: String): ContextLike = {
    val factory = getFactory(config)
    logger.info(s"Creating context $contextName from factory $factory.")
    factory.makeContext(config, contextName)
  }

  /**
   * Loads context factory from specified class
   * @param className context factory class
   * @return
   */
  def getFactory(className: String = defaultFactoryClassName): JobContextFactory = {
    logger.info(s"Loading context factory $className.")
    Class.forName(className).newInstance().asInstanceOf[JobContextFactory]
  }

  /**
   * Loads factory for specified configuration.
   * If config doesn't contain `context.job-context-factory` than [[JobContextFactory.defaultFactoryClassName]] will be loaded.
   * @param config config
   * @return
   */
  def getFactory(config: Config): JobContextFactory =
    getFactory(getValueFromConfig(config, classNameConfigEntry, defaultFactoryClassName))
}

