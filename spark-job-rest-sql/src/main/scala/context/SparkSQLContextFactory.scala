package context

import api.{ContextLike, SparkJobBase, SparkSqlJob}
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.slf4j.LoggerFactory

/**
 * Factory which creates simple SQL context.
 */
class SparkSQLContextFactory extends SQLContextFactory {
  type C = SQLContext with ContextLike
  val logger = LoggerFactory.getLogger(getClass)

  def makeContext(config: Config, sc: SparkContext): C = {
    logger.info(s"Creating SQL context for Spark context $sc.")
    new SQLContext(sc) with ContextLike {
      val contextClass = classOf[SQLContext].getName
      def isValidJob(job: SparkJobBase) = job.isInstanceOf[SparkSqlJob]
      def stop() = sparkContext.stop()
    }
  }
}
