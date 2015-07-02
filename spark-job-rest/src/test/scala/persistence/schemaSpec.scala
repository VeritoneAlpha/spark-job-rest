package persistence

import org.junit.runner.RunWith
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpec}
import persistence.schema._
import persistence.slickWrapper.Driver.api._
import test.durations.{dbTimeout, timeLimits}
import test.fixtures
import utils.schemaUtils.setupDatabaseSchema

import scala.concurrent.Await

/**
 * Test suit for database schema: [[schema]]
 */
@RunWith(classOf[JUnitRunner])
class schemaSpec extends WordSpec with MustMatchers with BeforeAndAfter with TimeLimitedTests {
  val timeLimit = timeLimits.dbTest

  implicit val timeout = dbTimeout

  val server = new DatabaseServer(fixtures.applicationConfig)
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
      val context = fixtures.contextEntity
      Await.result(db.run(contexts += context), timeout.duration)

      val insertedContext = Await.result(
        db.run(contexts.filter(_.id === context.id).result),
        timeout.duration
      ).head

      insertedContext.id mustEqual context.id
      insertedContext.name mustEqual context.name
      insertedContext.state mustEqual context.state
      insertedContext.submittedConfig.getString("hero.name") mustEqual context.submittedConfig.getString("hero.name")
      insertedContext.jars.list mustEqual context.jars.list
    }

    "describe `jobs` table" in {
      val context = fixtures.contextEntity
      val job = fixtures.jobEntity(context)
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
      insertedJob.runningClass mustEqual job.runningClass
      insertedJob.submittedConfig.getString("hero.name") mustEqual job.submittedConfig.getString("hero.name")
      insertedJob.finalConfig.get.getString("hero.name") mustEqual job.finalConfig.get.getString("hero.name")
    }
  }
}
