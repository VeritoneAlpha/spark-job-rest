package utils

import java.net.ServerSocket

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import config.durations
import org.slf4j.LoggerFactory
import server.domain.actors._
import server.domain.actors.messages.{Initialized, IsInitialized}

import scala.annotation.tailrec
import scala.concurrent.{Await, TimeoutException}
import scala.util.{Success, Try}


object ActorUtils {
  private val log = LoggerFactory.getLogger(getClass)

  private val askTimeout = durations.init.timeout
  private val reTries = durations.init.tries

  val PREFIX_CONTEXT_ACTOR = "A-"
  val PREFIX_CONTEXT_SYSTEM = "S-"

  val HOST_PROPERTY_NAME = "spark.job.rest.manager.akka.remote.netty.tcp.hostname"
  val PORT_PROPERTY_NAME = "spark.job.rest.manager.akka.remote.netty.tcp.port"

  def getContextActorAddress(contextName: String, host: String, port: Int): String ={
    getActorAddress(PREFIX_CONTEXT_SYSTEM + contextName, host, port, PREFIX_CONTEXT_ACTOR + contextName)
  }

  def getActorAddress(systemName: String, host: String, port: Int, actorName: String): String = {
    "akka.tcp://"  + systemName + "@" + host + ":" + port + "/user/" + actorName
  }

  /**
   * Finds available port looking up from given port number
   * @param port port to start search from
   * @return available port
   */
  @tailrec
  def findAvailablePort(port: Int): Integer = {
    Try {
      new ServerSocket(port).close()
    } match {
      case Success(_) => port
      case _ => findAvailablePort(port + 1)
    }
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
  final def awaitActorInitialization(actor: ActorRef, timeout: Timeout = askTimeout, tries: Int = reTries): Unit = tries match {
    case 0 =>
      throw new RuntimeException(s"Refused to wait for actor $actor initialization.")
    case _ =>
      implicit val askTimeout = timeout
      val future = actor ? IsInitialized
      // Await for future
      try { Await.ready(future, timeout.duration) }
      // Ignore timeout
      catch { case _: TimeoutException => }
      // Return if actor initialized or retry
      future.value.getOrElse(None) match {
        case Success(Initialized) =>
        case _ =>
          log.info(s"Actor $actor is not responding. Retrying.")
          awaitActorInitialization(actor, timeout, tries - 1)
      }
  }
}
