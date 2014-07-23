package spark.jobserver

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import java.net.{URLClassLoader}
import com.atigeo.jobmanager.JobManagerParameter
import java.io.File

class JobManagerActorCreator(param: JobManagerParameter, classLoader: URLClassLoader, port:Integer, configFileString: String) extends Runnable {

  val host = "localhost"
    val defaultConf = ConfigFactory.load()
    val configFile = new File(configFileString)
    val conf = ConfigFactory.parseFile(configFile).withFallback(defaultConf)

    val actorClass = classLoader.loadClass("spark.jobserver.JobManagerActor")
  def run() = {
    val config = remoteConfig(host, port, conf)
    println("Port is : " + port)
    implicit val system = ActorSystem(param.contextName, config, classLoader)

    val portJobServerSystem = conf.getInt("akka.remote.netty.tcp.port")
    println("portJobServerSystem is : " + portJobServerSystem)

    val jobDaoRef = system.actorSelection("akka.tcp://" + "JobServer" + "@localhost:" + portJobServerSystem + "/user/" + "jobDao");
    val localActor = system.actorOf(Props(actorClass, jobDaoRef, param.contextName, param.config, param.isAddHoc, param.resultActorRef, classLoader, configFileString), name = "JobManagerFor" + param.contextName)
    println("JobManagerActorCreator path " + localActor.path)
  }

  def remoteConfig(hostname: String, port: Int, commonConfig: Config): Config = {

      val configStr = """
      akka{
        actor {
            provider = "akka.remote.RemoteActorRefProvider"
        }
        remote {
          enabled-transports = ["akka.remote.netty.tcp"]
          log-sent-messages = on
          log-received-messages = on
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
