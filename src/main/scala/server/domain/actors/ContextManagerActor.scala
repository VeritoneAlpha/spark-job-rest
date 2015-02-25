package server.domain.actors

import java.io.File
import java.util

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.lang.exception.ExceptionUtils
import server.domain.actors.ContextActor.{FailedInit, InitializeContext, Initialized}
import server.domain.actors.ContextManagerActor._
import spray.http.StatusCodes

import scala.collection.mutable.{HashMap, SynchronizedMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Created by raduc on 03/11/14.
 */


object ContextManagerActor {

  case class CreateContext(contextName: String, user:String, jars: String, config: Config)
  case class ContextInitialized(port: String)
  case class DeleteContext(contextName: String)
  case class GetContext(contextName: String)
  case class GetAllContexts()
  case class NoSuchContext()
  case class ContextAlreadyExists()
  case class DestroyProcess(process: Process)
  case class IsAwake()

}

class ContextManagerActor(defaultConfig: Config) extends Actor with ActorLogging {

  var lastUsedPort = getValueFromConfig(defaultConfig, "appConf.actor.systems.first.port", 11000)
  var lastUsedPortSparkUi = getValueFromConfig(defaultConfig, "appConf.spark.ui.first.port", 16000)
  var firstPortForSparkExecutor =   getValueFromConfig(defaultConfig, "spark.executor.jmx.first.port", 21000)

  val contextMap = new HashMap[String, ActorSelection]() with SynchronizedMap[String, ActorSelection]
  val processMap = new HashMap[String, Process]() with SynchronizedMap[String, Process]

  val sparkUIConfigPath: String = "spark.ui.port"
  val sparkExtraJavaOptsPath = "spark.executor.extraJavaOptions"

  override def receive: Receive = {
    case CreateContext(contextName, user, jars, config) => {

      if(contextMap contains contextName) {
        sender ! ContextAlreadyExists
      } else if(jars.isEmpty){
        sender ! FailedInit("jars property is not defined or is empty.")
      } else {

        //adding the default configs
        var mergedConfig = config.withFallback(defaultConfig)

        //The port for the actor system
        val port = Util.findAvailablePort(lastUsedPort)
        lastUsedPort = port

        //If not defined, setting the spark.ui port
        if (!config.hasPath(sparkUIConfigPath)) {
          mergedConfig = addSparkUiPortToConfig(mergedConfig)
        }

        //If not defined, adding the Executor JMX port
        val jmxPort = Util.findAvailablePort(firstPortForSparkExecutor)
        if(!config.hasPath(sparkExtraJavaOptsPath)){
          mergedConfig = addSparkExecutorJMXPortToConfig(mergedConfig, jmxPort)
        }

        println(s"Received CreateContext message : context=$contextName jars=$jars")

        val processBuilder = createProcessBuilder(contextName, port, jars, mergedConfig, jmxPort, user)
        processMap += contextName -> processBuilder.start()

        val actorRef = context.actorSelection(Util.getContextActorAddress(contextName, port))
        sendInitMessage(contextName, port, actorRef, sender, mergedConfig)
      }

    }
      case DeleteContext(contextName) => {
      println(s"Received DeleteContext message : context=$contextName")
      if (contextMap contains contextName) {
        val contextRef = contextMap remove contextName get
        val processRef = processMap remove contextName get
        val future = contextRef ! DeleteContext(contextName)
        sender ! Success

        //If somehow the process didn't end
        scheduleDestroyMessage(processRef)

      } else {
        sender ! NoSuchContext
      }
    }

    case GetContext(contextName) => {
      println(s"Received GetContext message : context=$contextName")
      if (contextMap contains contextName) {
        sender ! contextMap(contextName)
      } else {
        sender ! NoSuchContext
      }
    }

    case GetAllContexts() => {
      println(s"Received GetAllContexts message.")
      sender ! contextMap.keys.mkString(",")
    }

  }

  def sendInitMessage(contextName: String, port: Int, actorRef: ActorSelection, sender: ActorRef, config:Config, counter: Int = 1):Unit = {

    val sleepTime = getValueFromConfig(config, "appConf.init.sleep", 3000)
    val sparkUiPort = config.getString(sparkUIConfigPath)

    context.system.scheduler.scheduleOnce(sleepTime millis) {

      val futureResult = context.actorOf(ReTry.props(tries = 20, retryTimeOut = 1000 millis, retryInterval = 1500 millis, actorRef)) ? IsAwake
      futureResult.onComplete{
        case Success(value) => {
          println(s"Got the callback, meaning = $value")

          val future = actorRef ? InitializeContext(contextName, config)
          future.onComplete {
            case Success(value) => {
              println(s"Got the callback, meaning = $value")
              value match {
                case Initialized => {
                  contextMap += contextName -> actorRef
                  sender ! ContextInitialized(sparkUiPort)
                }
                case e:FailedInit => {
                  println(s"Init failed for context $contextName");
                  sender ! e
                  processMap.remove(contextName).foreach(p => scheduleDestroyMessage(p))
                }
              }
            }
            case Failure(e) => {
              println("FAILED to send init message!")
              e.printStackTrace
                sender ! FailedInit(ExceptionUtils.getStackTrace(e))
                processMap.remove(contextName).get.destroy
            }
          }
        }
        case Failure(e) => {
          println("FAILED to send IsAwake message, the new Actor didn't initialize!")
        }
      }
    }
  }

  def addSparkUiPortToConfig(config: Config): Config = {
    lastUsedPortSparkUi = Util.findAvailablePort(lastUsedPortSparkUi)
    val map = new util.HashMap[String, String]()
    map.put(sparkUIConfigPath, lastUsedPortSparkUi.toString)
    val newConf = ConfigFactory.parseMap(map)
    newConf.withFallback(config)
  }

  def addSparkExecutorJMXPortToConfig(config: Config, jmxPort: Int): Config = {
    val map = new util.HashMap[String, String]()
    println(s"Executor JMX port is set to : $jmxPort")
    val configValue = "-Dcom.sun.management.jmxremote.port=" + jmxPort + " -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
    map.put(sparkExtraJavaOptsPath, configValue)
    val newConf = ConfigFactory.parseMap(map)
    newConf.withFallback(config)
  }

  def createProcessBuilder(contextName: String, port: Int, jars: String, config: Config, jmxPort: Int, user:String): ProcessBuilder = {

    val scriptPath = ContextManagerActor.getClass.getClassLoader.getResource("context_start.sh").getPath

    val xmxMemory = getValueFromConfig(config, "driver.xmxMemory", "1g")

    val pb = new ProcessBuilder(scriptPath, jars, contextName, port.toString, xmxMemory, jmxPort.toString, user)
    pb.redirectErrorStream(true)
    pb.redirectOutput(new File("logs/" + contextName + "-initializer.log"))
//    pb.redirectOutput()
  }

  def scheduleDestroyMessage(process: Process): Unit = {
    context.system.scheduler.scheduleOnce(5 seconds) {
      process.destroy()
    }
  }

}

