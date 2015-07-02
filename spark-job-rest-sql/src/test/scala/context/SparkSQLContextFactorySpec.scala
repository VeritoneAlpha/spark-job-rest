package context

import api.ContextLike
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.SQLContext
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.util.Try

/**
 * Test suite for [[HiveContextFactory]].
 */
@RunWith(classOf[JUnitRunner])
class SparkSQLContextFactorySpec extends WordSpec with MustMatchers with BeforeAndAfter {
  type C = SQLContext with ContextLike
  
  var sqlContext: C = _
  
  val sqlContextFactory = new SparkSQLContextFactory()
  
  // Clean up Spark context after each test
  after {
    Try{ sqlContext.stop() }
  }

  "SQLContextFactory" should {
    "create SQL context" in {
      sqlContext = sqlContextFactory.makeContext(config, this.getClass.getName)
      sqlContext.sparkContext.appName mustEqual this.getClass.getName
    }

    "stop underlying Spark context if context is stopped" in {
      sqlContext = sqlContextFactory.makeContext(config, "context1")
      sqlContext.stop()
      sqlContext = sqlContextFactory.makeContext(config, "context2")
      sqlContext.sparkContext.appName mustEqual "context2"
    }
  }

  val config = ConfigFactory.parseString(
    """
      |spark.master = "local"
      |spark.job.rest {
      |  context.jars = []
      |}
    """.stripMargin)
}
