package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Stash}
import akka.util.Timeout
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import persistence.DatabaseServer
import server.domain.actors.messages._
import utils.schemaUtils

/**
 * Database server actor responsible for starting database and providing connection info.
 * @param config database server config
 */
class DatabaseServerActor(config: Config) extends Actor with Stash {
  import context.become

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val log = LoggerFactory.getLogger(getClass)

  val server = new DatabaseServer(config).start()

  override def preStart(): Unit = self ! Init

  override def postStop(): Unit = server.stop()

  def initialised: Receive = {
    case GetDataBaseConnection =>
      log.info(s"Sending database connection ${server.db} to ${sender()}")
      sender() ! DatabaseConnection(server.db)
    case GetDatabaseInfo =>
      log.info(s"Sending database info ${server.databaseInfo} to ${sender()}")
      sender() ! server.databaseInfo
  }

  def receive: Receive = {
    case Init =>
      // Stash messages for later processing
      stash()
      // Synchronously create database schema
      schemaUtils.setupDatabaseSchema(server.db)
      // Switch to initialised mode and receive all messages
      become(initialised)
      unstashAll()
      log.info("Database server actor initialized.")
    case other =>
      log.warn(s"Received $other when not initialized.")
      sender() ! Uninitialized
  }
}
