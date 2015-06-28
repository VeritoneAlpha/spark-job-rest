package utils

import java.io.IOException
import java.net.ServerSocket

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import server.domain.actors._
import server.domain.actors.durations.defaultAskTimeout
import server.domain.actors.messages.{Initialized, IsInitialized}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.Success


object ActorUtils {

  val PREFIX_CONTEXT_ACTOR = "A-"
  val PREFIX_CONTEXT_SYSTEM = "S-"

  val HOST_PROPERTY_NAME = "manager.akka.remote.netty.tcp.hostname"
  val PORT_PROPERTY_NAME = "manager.akka.remote.netty.tcp.port"

  def getContextActorAddress(contextName: String, host: String, port: Int): String ={
    getActorAddress(PREFIX_CONTEXT_SYSTEM + contextName, host, port, PREFIX_CONTEXT_ACTOR + contextName)
  }

  def getActorAddress(systemName: String, host: String, port: Int, actorName: String): String = {
    "akka.tcp://"  + systemName + "@" + host + ":" + port + "/user/" + actorName
  }

  def findAvailablePort(lastUsedPort: Int): Integer = {
    val notFound = true
    var port = lastUsedPort + 1
    while (notFound) {
      try {
        new ServerSocket(port).close()
        return port
      }
      catch {
        case e: IOException =>
          port += 1
      }
    }
    return 0
  }

  def remoteConfig(hostname: String, port: Int, commonConfig: Config): Config = {

    val host = getValueFromConfig(commonConfig, ActorUtils.HOST_PROPERTY_NAME, "127.0.0.1")

    val configStr = """
      akka{
        log-dead-letters = 0
        actor {
            provider = "akka.remote.RemoteActorRefProvider"
        }
        remote {
          enabled-transports = ["akka.remote.netty.tcp"]
          log-sent-messages = on
          log-received-messages = on
          log-remote-lifecycle-events = off
          netty.tcp {
              maximum-frame-size = 512000b
              hostname = """" + host + """"
              port = """ + port +
      """ }
        }
      }"""

    ConfigFactory.parseString(configStr).withFallback(commonConfig)
  }

  /**
   * Blocks until actor will be initialized
   * @param actor actor reference
   * @param timeout timeout for each ask attempt
   * @param tries how many attempt should
   */
  @tailrec
  def awaitActorInitialization(actor: ActorRef, timeout: Timeout = defaultAskTimeout, tries: Int = 10): Unit = tries match {
    case 0 =>
      throw new RuntimeException(s"Refused to wait for actor $actor initialization.")
    case _ =>
      implicit val askTimeout = timeout
      val future = actor ? IsInitialized
      // Await for future
      Await.ready(future, timeout.duration)
      // Return if actor initialized or retry
      future.value.get match {
        case Success(Initialized) =>
        case _ => awaitActorInitialization(actor, timeout, tries - 1)
      }
  }
}
