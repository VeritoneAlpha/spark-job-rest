package server.domain.actors

import akka.actor.ActorSystem
import akka.pattern.{ask, gracefulStop}
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.junit.JUnitRunner
import persistence.schema
import persistence.schema._
import persistence.slickWrapper.Driver.api._
import server.domain.actors.messages._
import test.durations.{dbTimeout, timeLimits}
import test.fixtures

import scala.concurrent.Await
import scala.util.Success

/**
 * Test suit for [[ContextActor]]
 */
@RunWith(classOf[JUnitRunner])
class DatabaseServerActorSpec extends WordSpec with MustMatchers with BeforeAndAfter with TimeLimitedTests {
  val timeLimit = timeLimits.dbTest

  val config = ConfigFactory.load()

  implicit val timeout = dbTimeout
  implicit val system = ActorSystem("localSystem")

  var databaseServerActorRef: TestActorRef[DatabaseServerActor] = _
  def databaseServerActor = databaseServerActorRef.underlyingActor

  before {
    databaseServerActorRef = TestActorRef(new DatabaseServerActor(config))
  }

  after {
    Await.result(gracefulStop(databaseServerActorRef, timeout.duration), timeout.duration)
  }

  "DatabaseServerActor" should {
    "initialise database when started" in {
      val future = databaseServerActorRef ? GetDatabaseInfo
      val Success(DatabaseInfo(_, status, _, connectionString)) = future.value.get
      status must not equal null
      connectionString must not equal null
    }

    "provide database connection" in {
      val future = databaseServerActorRef ? GetDatabaseConnection
      val Success(DatabaseConnection(db)) = future.value.get
      val context = fixtures.contextEntity
      Await.result(db.run(contexts += context), timeout.duration)
      Await.result(db.run(schema.contexts.result), timeout.duration).size must not equal 0
    }
  }
}
