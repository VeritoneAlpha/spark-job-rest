package context

import api.{ContextLike, SparkJobBase}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.hive.HiveContext
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import utils.ContextUtils.configToSparkConf

import scala.util.Try

trait FakeContext

class FakeJobContextFactory extends JobContextFactory {
  type C = ContextLike
  def makeContext(config: Config, contextName: String): ContextLike = {
    val sparkConf = configToSparkConf(config, contextName)
    new SparkContext(sparkConf) with ContextLike with FakeContext {
      val contextClass = classOf[FakeContext].getName
      override def isValidJob(job: SparkJobBase): Boolean = true
      override def sparkContext: SparkContext = this
    }
  }
}

/**
 * Test suite for [[SQLContextFactory]].
 */
@RunWith(classOf[JUnitRunner])
class SQLContextFactorySpec extends WordSpec with MustMatchers with BeforeAndAfter {
  type C <: ContextLike
  
  var sqlContext: C = _
  
  // Clean up Spark context after each test
  after {
    Try{ sqlContext.stop() }
  }

  "SQLContextFactory" should {
    "create SQL context" in {
      sqlContext = JobContextFactory.makeContext(sqlContextFactoryConfig, "test").asInstanceOf[C]
      sqlContext.isInstanceOf[SQLContext] mustEqual true
      sqlContext.sparkContext.isInstanceOf[SparkContext] mustEqual true
      sqlContext.sparkContext.appName mustEqual "test"
    }

    "create SQL context on top of specified Spark context factory" in {
      sqlContext = JobContextFactory.makeContext(hiveSqlFactoryWithCustomSparkContextConfig, "test").asInstanceOf[C]
      sqlContext.isInstanceOf[HiveContext] mustEqual true
      sqlContext.sparkContext.isInstanceOf[FakeContext] mustEqual true
      sqlContext.sparkContext.appName mustEqual "test"
    }
  }

  val sqlContextFactoryConfig = ConfigFactory.parseString(
    """
      |spark.master = "local"
      |spark.app.id = "test"
      |spark.job.rest {
      |  context.jars = []
      |  context.job-context-factory = "context.SparkSQLContextFactory"
      |}
    """.stripMargin).resolve()

  val hiveSqlFactoryWithCustomSparkContextConfig = ConfigFactory.parseString(
    """
      |spark.master = "local"
      |spark.app.id = "test"
      |spark.job.rest {
      |  context.jars = []
      |  context.job-context-factory = "context.HiveContextFactory"
      |  context.spark-context-factory = "context.FakeJobContextFactory"
      |}
    """.stripMargin).resolve()
}
