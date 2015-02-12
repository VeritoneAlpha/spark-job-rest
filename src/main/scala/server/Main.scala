package server

import akka.event.Logging
import server.domain.actors._

import scala.concurrent.Await
import akka.actor.ActorRef
import akka.pattern.ask

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

/**
 * Created by raduc on 29/10/14.
 */
object Main {
  def main(args: Array[String]) {

    val defaultConfig = ConfigFactory.load()
    val masterConfig = defaultConfig.getConfig("manager")
    val system = ActorSystem("ManagerSystem", masterConfig)

    val supervisor = system.actorOf(Props(classOf[Supervisor]), "Supervisor")

    val jarManagerActor = createActor(Props(new JarManagerActor(defaultConfig)), "JarManager", system, supervisor)
    val contextManagerActor = createActor(Props(new ContextManagerActor(defaultConfig, jarManagerActor)), "ContextManager", system, supervisor)
    val jobManagerActor = createActor(Props(new JobActor(defaultConfig, jarManagerActor, contextManagerActor)), "JobManager", system, supervisor)
    new Controller(defaultConfig, jarManagerActor, contextManagerActor, jobManagerActor, system)
  }

  def createActor(props: Props, name: String, customSystem: ActorSystem, supervisor: ActorRef): ActorRef = {
    val actorRefFuture = ask(supervisor, (props, name))
    Await.result(actorRefFuture, timeout.duration).asInstanceOf[ActorRef]
  }
}
