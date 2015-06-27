package persistence

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpec}
import persistence.schema._
import persistence.slickWrapper.Driver.api._
import test.fixtures
import utils.schemaUtils._

import scala.concurrent.Await

/**
 * Test suit for database server: [[DatabaseServer]]
 */
@RunWith(classOf[JUnitRunner])
class DatabaseServerSpec extends WordSpec with MustMatchers with BeforeAndAfter {

  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

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

  "DatabaseServer" should {
    "provide database connection" in {
      Await.result(db.run(contexts += context), timeout.duration)
      Await.result(db.run(contexts.result), timeout.duration).nonEmpty mustEqual true
    }

    "clear database when reset" in {
      Await.result(db.run(contexts += context), timeout.duration)
      server.reset()
      server.start()
      setupDatabaseSchema(server.db)
      Await.result(db.run(contexts.result), timeout.duration).nonEmpty mustEqual false
    }
  }

  val context = fixtures.contextEntity
}
