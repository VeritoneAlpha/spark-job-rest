package server.domain.actors

import java.io.IOException
import java.net.{Socket, DatagramSocket, ServerSocket}

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created by raduc on 11/11/14.
 */
object Util {
  def createJmxConfigValue(jmxPort: Int): String = {
    "-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=" + jmxPort + " -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
  }


  val PREFIX_CONTEXT_ACTOR = "A-"
  val PREFIX_CONTEXT_SYSTEM = "S-"

  def getContextActorAddress(contextName: String, port: Int): String ={
    getActorAddress(PREFIX_CONTEXT_SYSTEM + contextName, port, PREFIX_CONTEXT_ACTOR + contextName)
  }

  def getActorAddress(systemName: String, port: Int, actorName: String): String = {
    "akka.tcp://"  + systemName + "@localhost:" + port + "/user/" + actorName
  }

  def findAvailablePort(lastUsedPort: Int): Integer = {
    var ss:ServerSocket = null
    var ds:DatagramSocket = null

    val notFound = true
    var port = lastUsedPort + 1
    while (notFound) {
      try {
        ss = new ServerSocket(port)
        ss.setReuseAddress(true)
        ds = new DatagramSocket(port)
        ds.setReuseAddress(true)
        return port
      }
      catch {
        case e: IOException => {
          port += 1
        }
      } finally {
        if (ds != null) {
          ds.close()
        }

        if (ss != null) {
          try {
            ss.close()
          } catch {
            case e: IOException => println("Exception when closing port.")
          }
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
