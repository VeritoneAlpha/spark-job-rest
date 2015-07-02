package context

import api.ContextLike
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.hive.HiveContext
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.util.Try

/**
 * Test suite for [[HiveContextFactory]].
 */
@RunWith(classOf[JUnitRunner])
class HiveContextFactorySpec extends WordSpec with MustMatchers with BeforeAndAfter {
  type C = HiveContext with ContextLike
  
  var hiveContext: C = _
  
  val hiveContextFactory = new HiveContextFactory()
  
  // Clean Spark context after each test
  after {
    Try{ hiveContext.stop() }
  }

  "HiveContextFactory" should {
    "create Hive context" in {
      hiveContext = hiveContextFactory.makeContext(config, this.getClass.getName)
      hiveContext.sparkContext.appName mustEqual this.getClass.getName
    }

    "stop underlying Spark context if context is stopped" in {
      hiveContext = hiveContextFactory.makeContext(config, "context1")
      hiveContext.stop()
      hiveContext = hiveContextFactory.makeContext(config, "context2")
      hiveContext.sparkContext.appName mustEqual "context2"
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
