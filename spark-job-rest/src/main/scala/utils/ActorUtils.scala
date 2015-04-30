package utils

import java.io.IOException
import java.net.ServerSocket

import com.typesafe.config.{Config, ConfigFactory}
import server.domain.actors._

/**
 * Created by raduc on 11/11/14.
 */
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
    val notFound = true;
    var port = lastUsedPort + 1
    while (notFound) {
      try {
        new ServerSocket(port).close()
        return port
      }
      catch {
        case e: IOException => {
          port += 1
        }
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
}
