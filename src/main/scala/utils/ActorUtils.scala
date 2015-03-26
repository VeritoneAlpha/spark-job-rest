package utils

import java.io.IOException
import java.net.ServerSocket

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created by raduc on 11/11/14.
 */
object ActorUtils {

  val PREFIX_CONTEXT_ACTOR = "A-"
  val PREFIX_CONTEXT_SYSTEM = "S-"

  def getContextActorAddress(contextName: String, port: Int): String ={
    getActorAddress(PREFIX_CONTEXT_SYSTEM + contextName, port, PREFIX_CONTEXT_ACTOR + contextName)
  }

  def getActorAddress(systemName: String, port: Int, actorName: String): String = {
    "akka.tcp://"  + systemName + "@localhost:" + port + "/user/" + actorName
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
              hostname = "localhost"
              port = """ + port +
      """ }
        }
      }"""

    ConfigFactory.parseString(configStr).withFallback(commonConfig)
  }
}
