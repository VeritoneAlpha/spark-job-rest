package server

import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import server.domain.actors.ContextActor
import utils.ActorUtils

/**
* Created by raduc on 30/10/14.
*/
object MainContext {

  def main(args: Array[String]) {
    //args(0) - appConf , args(1) - jars
    val confFilePath = args(0)
    val jarsPath = args(1).split(":")
    val contextName = args(2)
    val port = args(3).toInt

    println(s"Started new process for contextName = $contextName with port = $port")

    val defaultConfig = ConfigFactory.load()
    val config = ActorUtils.remoteConfig("localhost", port, defaultConfig)
    val system = ActorSystem(ActorUtils.PREFIX_CONTEXT_SYSTEM + contextName, config)

    system.actorOf(Props(new ContextActor(defaultConfig)), ActorUtils.PREFIX_CONTEXT_ACTOR + contextName)

    println(s"Initialized system $ActorUtils.PREFIX_CONTEXT_SYSTEM$contextName and actor $ActorUtils.PREFIX_CONTEXT_SYSTEM$contextName")

  }

}
