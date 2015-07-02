package server.domain.actors

import akka.actor.Actor
import com.typesafe.config.Config
import config.durations.AskTimeout
import org.slf4j.LoggerFactory
import persistence.DatabaseServer
import server.domain.actors.messages._
import utils.schemaUtils

/**
 * Database server actor responsible for starting database and providing connection info.
 * @param config database server config
 */
class DatabaseServerActor(val config: Config) extends Actor with AskTimeout {
  val log = LoggerFactory.getLogger(getClass)

  log.info(s"Creating database server instance.")
  val server = new DatabaseServer(config)

  override def preStart(): Unit = {
    log.info("Starting database server.")
    server.start()
    log.info("Setting up database schema.")
    schemaUtils.setupDatabaseSchema(server.db)
    log.info("Database server actor initialized.")
  }

  override def postStop(): Unit = server.stop()

  def receive: Receive = {
    case GetDatabaseConnection =>
      log.info(s"Sending database connection ${server.db} to ${sender()}")
      sender() ! DatabaseConnection(server.db)
    case GetDatabaseInfo =>
      log.info(s"Sending database info ${server.databaseInfo} to ${sender()}")
      sender() ! server.databaseInfo
    case IsInitialized =>
      sender() ! Initialized
  }
}
