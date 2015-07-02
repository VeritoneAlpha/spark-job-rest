package persistence

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import server.domain.actors.DatabaseServerActor
import server.domain.actors.messages._
import test.durations.dbTimeout
import utils.ActorUtils.awaitActorInitialization

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Entry point for test application that does nothing but starts embed database server and waits for connection. 
 */
object DatabaseServerExec {
  val config = ConfigFactory.load("deploy").withFallback(ConfigFactory.load())

  def main (args: Array[String]) {
    implicit val timeout = dbTimeout

    val system = ActorSystem("DatabaseServer", config)

    val databaseServerActor = system.actorOf(Props(new DatabaseServerActor(config)))
    awaitActorInitialization(databaseServerActor, dbTimeout)

    for (dbInfo <- databaseServerActor ? GetDatabaseInfo)
      println(s"Started database server: $dbInfo")
  }
}
