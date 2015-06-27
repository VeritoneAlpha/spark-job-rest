package server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import logging.LoggingOutputStream
import server.domain.actors._

import scala.concurrent.Await

/**
 * Spark-Job-REST entry point.
 */
object Main {
  def main(args: Array[String]) {

    LoggingOutputStream.redirectConsoleOutput

    // Loads deployment configuration `deploy.conf` on top of application defaults `application.conf`
    val defaultConfig = ConfigFactory.load("deploy").withFallback(ConfigFactory.load())

    val masterConfig = defaultConfig.getConfig("manager")
    val system = ActorSystem("ManagerSystem", masterConfig)

    val supervisor = system.actorOf(Props(classOf[Supervisor]), "Supervisor")

    val jarActor = createActor(Props(new JarActor(defaultConfig)), "JarActor", system, supervisor)
    val contextManagerActor = createActor(Props(new ContextManagerActor(defaultConfig, jarActor)), "ContextManager", system, supervisor)
    val jobManagerActor = createActor(Props(new JobActor(defaultConfig, contextManagerActor)), "JobManager", system, supervisor)

    // HTTP server will start immediately after controller instantiation
    new Controller(defaultConfig, contextManagerActor, jobManagerActor, jarActor, system)
  }

  def createActor(props: Props, name: String, customSystem: ActorSystem, supervisor: ActorRef): ActorRef = {
    val actorRefFuture = ask(supervisor, (props, name))
    Await.result(actorRefFuture, timeout.duration).asInstanceOf[ActorRef]
  }
}
