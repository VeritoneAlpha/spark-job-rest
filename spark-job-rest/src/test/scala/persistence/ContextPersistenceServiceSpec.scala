package persistence

import api.entities.ContextState._
import org.junit.runner.RunWith
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpec}
import persistence.schema._
import persistence.services.ContextPersistenceService._
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

  val config = fixtures.applicationConfig

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
      val (_, finalContext) = createAndUpdateThrough(Requested, Running, "awesome jump")
      finalContext.state mustEqual Running
      finalContext.details mustEqual "awesome jump"
    }

    "not change context state if it is failed" in {
      val (_, finalContext) = createAndUpdateThrough(Failed, Running)
      finalContext.state mustEqual Failed
    }

    "not change context state if it is stopped" in {
      val (_, finalContext) = createAndUpdateThrough(Terminated, Running)
      finalContext.state mustEqual Terminated
    }
  }

  def createAndUpdateThrough(through: ContextState, to: ContextState, lastDetails: String = "") = {
    val initialContext = fixtures.contextEntity
    Await.result(db.run(contexts += initialContext), timeout.duration)

    updateContextState(initialContext.id, through, db)
    updateContextState(initialContext.id, to, db, lastDetails)

    val finalContext = Await.result(
      db.run(contexts.filter(_.id === initialContext.id).result),
      timeout.duration
    ).head

    (initialContext, finalContext)
  }
}
