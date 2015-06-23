package context

import api.{ContextLike, SparkJobBase, SparkSqlJob}
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.sql.hive.HiveContext
import org.slf4j.LoggerFactory

/**
 * Factory which creates Hive context.
 */
class HiveContextFactory extends SQLContextFactory {
  type C = HiveContext with ContextLike
  val logger = LoggerFactory.getLogger(getClass)

  def makeContext(config: Config, sc: SparkContext): C = {
    logger.info(s"Creating Hive context for Spark context $sc.")
    new HiveContext(sc) with ContextLike {
      val contextClass = classOf[HiveContext].getName
      def isValidJob(job: SparkJobBase) = job.isInstanceOf[SparkSqlJob]
      def stop() = sparkContext.stop()
    }
  }
}
