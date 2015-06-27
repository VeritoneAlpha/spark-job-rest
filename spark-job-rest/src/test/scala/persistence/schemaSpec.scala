package persistence

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.SpanSugar._
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpec}
import persistence.schema._
import persistence.slickWrapper.Driver.api._
import utils.schemaUtils.setupDatabaseSchema

import scala.concurrent.Await

/**
 * Test suit for database schema: [[schema]]
 */
@RunWith(classOf[JUnitRunner])
class schemaSpec extends WordSpec with MustMatchers with BeforeAndAfter with TimeLimitedTests {
  val timeLimit = 3.seconds

  implicit val timeout = Timeout(3, TimeUnit.SECONDS)

  val config = ConfigFactory.load()
  val server = new DatabaseServer(config)
  server.reset()
  def db = server.db

  before {
    server.start()
    setupDatabaseSchema(server.db, resetSchema = true)
  }

  after {
    server.reset()
  }

  "database schema" should {
    "describe `contexts` table" in {
      val context = Context("test context", ContextState.Running, bananaConfig, Jars(List("foo", "bar")), nextId)

      Await.result(db.run(contexts += context), timeout.duration)

      val insertedContext = Await.result(
        db.run(contexts.filter(_.id === context.id).result),
        timeout.duration
      ).head

      insertedContext.id mustEqual context.id
      insertedContext.name mustEqual "test context"
      insertedContext.state mustEqual ContextState.Running
      insertedContext.config.getString("hero.name") mustEqual "Geoffrey Pirate Prentice"
      insertedContext.jars.list mustEqual context.jars.list
    }

    "describe `jobs` table" in {
      val context = Context("test context", ContextState.Running, bananaConfig, Jars(), nextId)
      val job = Job(Some(context.id), None, None, "java.utils.UUID", diningConfig, bananaConfig)

      val insertCommands = DBIO.seq(
        contexts += context,
        jobs += job
      )
      Await.result(db.run(insertCommands), timeout.duration)

      val insertedJob = Await.result(
        db.run(jobs.filter(_.id === job.id).result),
        timeout.duration
      ).head

      insertedJob.contextId.get mustEqual context.id
      insertedJob.status mustEqual job.status
      insertedJob.runningClass mustEqual "java.utils.UUID"
      insertedJob.submittedConfig.getString("hero.name") mustEqual "Lizzy"
      insertedJob.finalConfig.getString("hero.name") mustEqual "Geoffrey Pirate Prentice"
    }
  }

  /**
   * A small config
   */
  val diningConfig = ConfigFactory.parseString(
    """
      |{
      |  hero.name = "Lizzy"
      |}
    """.stripMargin)

  /**
   * Just a random big config. But:
   * Yay! I've just got that "Gravity's Rainbow" is a sequel of V. since it's about V-2 (how I've missed that before?)
   * @author Mikhail Zyatin
   */
  val bananaConfig = ConfigFactory.parseString(
    """
      |{
      |  hero.name = "Geoffrey Pirate Prentice"
      |}
    """.stripMargin)
    // We want to test against a BIG config
    .withFallback(config)
}
