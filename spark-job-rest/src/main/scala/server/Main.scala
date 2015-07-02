package server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import config.durations.AskTimeout
import config.{default, master}
import logging.LoggingOutputStream
import server.domain.actors._
import utils.ActorUtils.awaitActorInitialization

import scala.concurrent.Await

/**
 * Spark-Job-REST entry point.
 */
object Main extends AskTimeout {
  LoggingOutputStream.redirectConsoleOutput

  def main(args: Array[String]) {
    // Use default config as a base
    val config = default
    // Get master config
    val masterConfig = master

    val system = ActorSystem("ManagerSystem", masterConfig)

    val supervisor = system.actorOf(Props(classOf[Supervisor]), "Supervisor")

    // Database server actor will instantiate database and ensures that schema is created
    val databaseServerActor = createActor(Props(new DatabaseServerActor(config)), "DatabaseServerActor", system, supervisor)
    // We should wait for this actor to be initialized before proceed
    awaitActorInitialization(databaseServerActor)

    val jarActor = createActor(Props(new JarActor(config)), "JarActor", system, supervisor)
    val contextManagerActor = createActor(Props(new ContextManagerActor(config, jarActor, databaseServerActor)), "ContextManager", system, supervisor)
    val jobManagerActor = createActor(Props(new JobActor(config, contextManagerActor, databaseServerActor)), "JobManager", system, supervisor)

    // HTTP server will start immediately after controller instantiation
    new Controller(config, contextManagerActor, jobManagerActor, jarActor, databaseServerActor, system)
  }

  def createActor(props: Props, name: String, customSystem: ActorSystem, supervisor: ActorRef): ActorRef = {
    val actorRefFuture = ask(supervisor, (props, name))
    Await.result(actorRefFuture, timeout.duration).asInstanceOf[ActorRef]
  }
}
