package server

import akka.actor.{ActorSystem, Props}
import config.default
import logging.LoggingOutputStream
import org.slf4j.LoggerFactory
import server.domain.actors.ContextActor
import utils.ActorUtils

/**
 * Spark context container entry point.
 */
object MainContext {

  LoggingOutputStream.redirectConsoleOutput
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val contextName = System.getenv("SPARK_JOB_REST_CONTEXT_NAME")
    val port = System.getenv("SPARK_JOB_REST_CONTEXT_PORT").toInt

    log.info(s"Started new process for contextName = $contextName with port = $port")

    // Use default config as a base
    val defaultConfig = default
    val config = ActorUtils.remoteConfig("localhost", port, defaultConfig)
    val system = ActorSystem(ActorUtils.PREFIX_CONTEXT_SYSTEM + contextName, config)

    system.actorOf(Props(new ContextActor(defaultConfig)), ActorUtils.PREFIX_CONTEXT_ACTOR + contextName)

    log.info(s"Initialized system ${ActorUtils.PREFIX_CONTEXT_SYSTEM}$contextName and actor ${ActorUtils.PREFIX_CONTEXT_SYSTEM}$contextName")
  }
}
