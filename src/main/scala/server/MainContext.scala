package server

import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import server.domain.actors.{Util, ContextActor}

/**
* Created by raduc on 30/10/14.
*/
object MainContext {

  def main(args: Array[String]) {
    //args(0) - appConf
    val confFilePath = args(0)
    // val jarsPath = args(1).split(":")
    val contextName = args(1)
    val port = args(2).toInt

    println(s"Started new process for contextName = $contextName with port = $port")

    val defaultConfig = ConfigFactory.load()
    val config = Util.remoteConfig("localhost", port, defaultConfig)
    val system = ActorSystem(Util.PREFIX_CONTEXT_SYSTEM + contextName, config)

    system.actorOf(Props(new ContextActor(defaultConfig)), Util.PREFIX_CONTEXT_ACTOR + contextName)

    println(s"Initialized system $Util.PREFIX_CONTEXT_SYSTEM$contextName and actor $Util.PREFIX_CONTEXT_SYSTEM$contextName")

  }

}
