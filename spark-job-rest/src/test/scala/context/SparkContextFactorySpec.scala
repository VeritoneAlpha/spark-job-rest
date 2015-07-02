package context

import api.ContextLike
import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkContext
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.util.Try

/**
 * Test suite for [[SparkContextFactory]].
 */
@RunWith(classOf[JUnitRunner])
class SparkContextFactorySpec extends WordSpec with MustMatchers with BeforeAndAfter {
  type C = SparkContext with ContextLike

  var sparkContext: C = _
  val sparkContextFactory = new SparkContextFactory()

  // Destroy Spark context after each test
  after {
    Try{ sparkContext.stop() }
  }

  "SingletonSparkContextFactory" should {
    "create Spark context" in {
      sparkContext = sparkContextFactory.makeContext(config, this.getClass.getName)
      sparkContext.appName mustEqual this.getClass.getName
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
