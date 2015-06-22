package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import context.{FakeContext, JobContextFactory}
import org.apache.spark.SparkContext
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.SpanSugar._

import scala.util.Success

/**
 * Test suit for [[ContextActor]]
 */
@RunWith(classOf[JUnitRunner])
class ContextActorSpec extends WordSpec with MustMatchers with BeforeAndAfter with TimeLimitedTests {
  val timeLimit = 10 seconds

  val config = ConfigFactory.load()

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)
  implicit val system = ActorSystem("localSystem")

  var contextActorRef: TestActorRef[ContextActor] = _
  def contextActor = contextActorRef.underlyingActor

  val contextName = "demoContext"

  before {
    contextActorRef = TestActorRef(new ContextActor(config))
  }

  after {
    contextActor.jobContext.stop()
  }

  "ContextActor" should {
    "create Spark context when initialized" in {
      val future = contextActorRef ? ContextActor.Initialize(contextName, config, List())
      val Success(result: ContextActor.Initialized) = future.value.get
      result must not equal null
      contextActor.jobContext.isInstanceOf[SparkContext] mustEqual true
    }

    "have default factory for Spark context" in {
      val configWithoutFactory = config.withoutPath(JobContextFactory.classNameConfigEntry)
      val future = contextActorRef ? ContextActor.Initialize(contextName, configWithoutFactory, List())
      val Success(result: ContextActor.Initialized) = future.value.get
      result must not equal null
      contextActor.jobContext.isInstanceOf[SparkContext] mustEqual true
    }

    "create context from specified factory" in {
      val future = contextActorRef ? ContextActor.Initialize(contextName, fakeContextFactoryConfig, List())
      val Success(result: ContextActor.Initialized) = future.value.get
      result must not equal null
      contextActor.jobContext.isInstanceOf[FakeContext] mustEqual true
    }
  }

  val fakeContextFactoryConfig = ConfigFactory.parseString(
    """
      |{
      |  context.job-context-factory = "context.FakeJobContextFactory",
      |}
    """.stripMargin).withFallback(config)
}
