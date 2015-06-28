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
class ContextPersistenceServiceSpec extends WordSpec with MustMatchers with BeforeAndAfter with TimeLimitedTests {
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

  "ContextPersistenceService" should {
    "update context state" in {
      val context = fixtures.contextEntity
      Await.result(db.run(contexts += context), timeout.duration)

      ContextPersistenceService.updateContextState(context.id, ContextState.Stopped, db)

      val finalContext = Await.result(
        db.run(contexts.filter(_.id === context.id).result),
        timeout.duration
      ).head

      finalContext.state mustEqual ContextState.Stopped
    }

    "not change context state if it is failed" in {
      val context = fixtures.contextEntity
      Await.result(db.run(contexts += context), timeout.duration)

      ContextPersistenceService.updateContextState(context.id, ContextState.Failed, db)
      ContextPersistenceService.updateContextState(context.id, ContextState.Stopped, db)

      val finalContext = Await.result(
        db.run(contexts.filter(_.id === context.id).result),
        timeout.duration
      ).head

      finalContext.state mustEqual ContextState.Failed
    }
  }
}
