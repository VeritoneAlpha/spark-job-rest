package server.domain.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import persistence.slickWrapper.Driver.api._
import server.domain.actors.messages._

import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Database connection actor responsible for requesting with database info from connection provider actor and
 * constructing database connection which may be shipped to interested clients.
 * Also database connection can pass database info to remote actors to let them create their own connections.
 * Which means that both [[DatabaseServerActor]] and [[DatabaseServerActor]] can be a connection provider.
 *
 * @param connectionProviderActor reference to actor which has database connection
 * @param config application level config
 */
class DatabaseConnectionActor(connectionProviderActor: ActorRef, val config: Config) extends Actor {

  val log = LoggerFactory.getLogger(getClass)

  def receive: Receive = {
    case GetDatabaseConnection =>
      val client = sender()
      for (databaseInfo <- (self ? GetDatabaseInfo).mapTo[DatabaseInfo]) {
        val db = Database.forURL(url = databaseInfo.connectionString)
        log.info(s"Sending database connection $db to $client")
        client ! DatabaseConnection(db)
      }
    case GetDatabaseInfo =>
      val client = sender()
      for (databaseInfo <- (connectionProviderActor ? GetDatabaseInfo).mapTo[DatabaseInfo]) {
        log.info(s"Sending database info $databaseInfo to $client")
        client ! databaseInfo
      }
  }
}
