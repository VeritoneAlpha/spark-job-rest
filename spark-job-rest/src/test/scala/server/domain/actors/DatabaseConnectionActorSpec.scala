package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.{ask, gracefulStop}
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.SpanSugar._
import persistence.schema
import persistence.schema._
import persistence.slickWrapper.Driver.api._
import server.domain.actors.messages._

import scala.concurrent.Await

/**
 * Test suit for [[ContextActor]]
 */
@RunWith(classOf[JUnitRunner])
class DatabaseConnectionActorSpec extends WordSpec with MustMatchers with BeforeAndAfter with TimeLimitedTests {
  val timeLimit = 3.seconds

  val config = ConfigFactory.load()

  implicit val timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val system = ActorSystem("localSystem")

  var databaseServerActorRef: TestActorRef[DatabaseServerActor] = _
  var databaseConnectionActorRef: TestActorRef[DatabaseConnectionActor] = _

  before {
    databaseServerActorRef = TestActorRef(new DatabaseServerActor(config))
    databaseConnectionActorRef = TestActorRef(new DatabaseConnectionActor(databaseServerActorRef))
  }

  after {
    Await.result(gracefulStop(databaseServerActorRef, timeout.duration), timeout.duration)
    Await.result(gracefulStop(databaseConnectionActorRef, timeout.duration), timeout.duration)
  }

  "DatabaseConnectionActor" should {
    "provide database info" in {
      val DatabaseInfo(_, status, _, connectionString) = Await.result(databaseConnectionActorRef ? GetDatabaseInfo, timeout.duration)
      status must not equal null
      connectionString must not equal null
    }

    "provide database connection" in {
      val DatabaseConnection(db) = Await.result(databaseConnectionActorRef ? GetDataBaseConnection, timeout.duration)
      val context = Context("test context", ContextState.Running, config, Jars(List("foo", "bar")), nextId)
      Await.result(db.run(contexts += context), timeout.duration)
      Await.result(db.run(schema.contexts.result), timeout.duration).size must not equal 0
    }
  }
}
