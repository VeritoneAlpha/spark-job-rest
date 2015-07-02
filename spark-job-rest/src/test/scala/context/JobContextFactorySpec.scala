package context

import api.{ContextLike, SparkJobBase}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.SparkContext
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

trait FakeContext

class FakeJobContextFactory extends JobContextFactory {
  type C = ContextLike
  def makeContext(config: Config, contextName: String): ContextLike = new ContextLike with FakeContext {
    val contextClass = classOf[FakeContext].getName
    override def stop(): Unit = {}
    override def isValidJob(job: SparkJobBase): Boolean = true
    override def sparkContext: SparkContext = null
  }
}

/**
 * Test suite for [[JobContextFactory]].
 */
@RunWith(classOf[JUnitRunner])
class JobContextFactorySpec extends WordSpec with MustMatchers with BeforeAndAfter {
  "JobContextFactory" should {
    "load specified factory" in {
      JobContextFactory
        .getFactory("context.SparkContextFactory")
        .isInstanceOf[SparkContextFactory] mustEqual true
    }

    "load default factory" in {
      JobContextFactory
        .getFactory()
        .isInstanceOf[SparkContextFactory] mustEqual true
    }

    "make context with default factory if other is not specified" in {
      val context = JobContextFactory.makeContext(ConfigFactory.parseString(
        """
        |spark.master = "local",
        |spark.app.id = "test"
        |
        |spark.job.rest {
        |  context.jars = []
        |}
        """.stripMargin).resolve(), "test")
      context.isInstanceOf[SparkContext] mustEqual true
      context.stop()
    }

    "make context with specified factory if other is not specified" in {
      JobContextFactory.makeContext(ConfigFactory.parseString(
        """
          |spark.master = "local",
          |spark.app.id = "test"
          |
          |spark.job.rest {
          |  context.jars = [],
          |  context.job-context-factory = "context.FakeJobContextFactory"
          |}
        """.stripMargin).resolve(), "test")
        .isInstanceOf[FakeContext] mustEqual true
    }
  }
}
