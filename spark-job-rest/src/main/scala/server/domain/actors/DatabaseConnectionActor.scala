package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import org.slf4j.LoggerFactory
import persistence.slickWrapper.Driver.api._
import server.domain.actors.messages._

import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Database connection actor responsible for negotiating with database server actor for database info and
 * constructing database connection which it ships to all interested clients.
 * Also database connection can pass database info to remote actors to let them create their own connections.
 *
 * @param databaseServerActor reference to database actor
 */
class DatabaseConnectionActor(databaseServerActor: ActorRef) extends Actor {
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val log = LoggerFactory.getLogger(getClass)

  def receive: Receive = {
    case GetDataBaseConnection =>
      val client = sender()
      for (databaseInfo <- (self ? GetDatabaseInfo).mapTo[DatabaseInfo]) {
        val db = Database.forURL(url = databaseInfo.connectionString)
        log.info(s"Sending database connection $db to $client")
        client ! DatabaseConnection(db)
      }
    case GetDatabaseInfo =>
      val client = sender()
      for (databaseInfo <- (databaseServerActor ? GetDatabaseInfo).mapTo[DatabaseInfo]) {
        log.info(s"Sending database info $databaseInfo to $client")
        client ! databaseInfo
      }
  }
}
