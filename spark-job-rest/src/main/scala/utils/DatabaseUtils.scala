package utils

import akka.actor.ActorRef
import akka.pattern.ask
import server.domain.actors.durations
import server.domain.actors.messages._

import scala.concurrent.Await

object DatabaseUtils {
  implicit val timeout = durations.defaultRemoteAskTimeout

  /**
   * Synchronously requests connection from connection provider actor which may be either
   * [[server.domain.actors.DatabaseServerActor]] or [[server.domain.actors.DatabaseConnectionActor]]
   * @param connectionProviderActor connection provider actor
   * @return connection
   */
  def dbConnection(connectionProviderActor: ActorRef) = {
    val DatabaseConnection(db) = Await.result(connectionProviderActor ? GetDatabaseConnection, timeout.duration)
    db
  }
}
