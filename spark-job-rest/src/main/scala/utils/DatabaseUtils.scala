package utils

import akka.actor.ActorRef
import akka.pattern.ask
import config.durations
import persistence.slickWrapper.Driver.api.Database
import server.domain.actors.messages._

import scala.annotation.tailrec
import scala.concurrent.{Await, TimeoutException}
import scala.util.{Failure, Success, Try}

object DatabaseUtils {
  private implicit lazy val timeout = durations.db.connection.timeout
  private lazy val maxReties = durations.db.connection.tries

  /**
   * Synchronously requests connection from connection provider actor which may be either.
   * [[server.domain.actors.DatabaseServerActor]] or [[server.domain.actors.DatabaseConnectionActor]]
   * @param connectionProviderActor connection provider actor
   * @param retries optional retries to be performed
   * @throws TimeoutException if tried everything
   * @return connection
   */
  @tailrec
  final def dbConnection(connectionProviderActor: ActorRef, retries: Int = maxReties): Database = {
    Try {
      Await.result(connectionProviderActor ? GetDatabaseConnection, timeout.duration)
    } match {
      case Success(DatabaseConnection(db)) => db
      case Failure(e: TimeoutException) =>
        if (retries == 0)
          throw new TimeoutException("Failed to connect to database.")
        else
          dbConnection(connectionProviderActor, retries)
    }
  }
}
