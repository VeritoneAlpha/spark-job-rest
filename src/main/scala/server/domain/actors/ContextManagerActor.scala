package server.domain.actors

import java.io.File
import java.util

import scala.collection.mutable.{HashMap, SynchronizedMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.log4j.Logger

import server.domain.actors.ContextActor.{FailedInit, InitializeContext, Initialized}
import server.domain.actors.ContextManagerActor._

/**
 * Created by raduc on 03/11/14.
 */
object ContextManagerActor {

  case class CreateContext(contextName: String, config: Config)
  case class ContextInitialized(port: String)
  case class DeleteContext(contextName: String)
  case class GetContext(contextName: String)
  case class GetAllContexts()
  case class NoSuchContext()
  case class ContextAlreadyExists()
  case class DestroyProcess(process: Process)
  case class IsAwake()

}

class ContextManagerActor(defaultConfig: Config, jarManagerActor: ActorRef) extends Actor with ActorLogging {
  private val logger = Logger.getLogger(getClass)

  var lastUsedPort = getValueFromConfig(defaultConfig, "appConf.actor.systems.first.port", 11000)
  var lastUsedPortSparkUi = getValueFromConfig(defaultConfig, "appConf.spark.ui.first.port", 16000)

  val contextMap = new HashMap[String, ActorSelection]() with SynchronizedMap[String, ActorSelection]
  val processMap = new HashMap[String, Process]() with SynchronizedMap[String, Process]

  val sparkUIConfigPath: String = "spark.ui.port"

  override def receive: Receive = {
    case CreateContext(contextName, config) => {

      if(contextMap contains contextName) {
        sender ! ContextAlreadyExists
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

        logger.info(s"Received CreateContext message : context=$contextName")

        val processBuilder = createProcessBuilder(contextName, port, mergedConfig)
        processMap += contextName -> processBuilder.start()

        val actorRef = context.actorSelection(Util.getContextActorAddress(contextName, port))
        sendInitMessage(contextName, port, actorRef, sender, mergedConfig)
      }
    }
      case DeleteContext(contextName) => {
        logger.info(s"Received DeleteContext message : context=$contextName")
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
      logger.info(s"Received GetContext message : context=$contextName")
      if (contextMap contains contextName) {
        sender ! contextMap(contextName)
      } else {
        sender ! NoSuchContext
      }
    }

    case GetAllContexts() => {
      logger.info(s"Received GetAllContexts message.")
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
          logger.info(s"Got the callback, meaning = $value")

          val future = actorRef ? InitializeContext(contextName, config)
          future.onComplete {
            case Success(value) => {
              logger.info(s"Got the callback, meaning = $value")
              value match {
                case Initialized => {
                  contextMap += contextName -> actorRef
                  sender ! ContextInitialized(sparkUiPort)
                }
                case e:FailedInit => {
                  logger.info(s"Init failed for context $contextName");
                  sender ! e
                  processMap.remove(contextName).foreach(p => scheduleDestroyMessage(p))
                }
              }
            }
            case Failure(e) => {
              logger.error("FAILED to send init message!", e)
              sender ! FailedInit(ExceptionUtils.getStackTrace(e))
              processMap.remove(contextName).get.destroy
            }
          }
        }
        case Failure(e) => {
          logger.info("FAILED to send IsAwake message, the new Actor didn't initialize!")
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

  def createProcessBuilder(contextName: String, port: Int, config: Config): ProcessBuilder = {

    val scriptPath = ContextManagerActor.getClass.getClassLoader.getResource("context_start.sh").getPath

    val xmxMemory = getValueFromConfig(config, "driver.xmxMemory", "1g")

    val pb = new ProcessBuilder(scriptPath, contextName, port.toString, xmxMemory)
    pb.redirectErrorStream(true)
    pb.redirectOutput(new File("logs/" + contextName + "-initializer.log"))
  }

  def scheduleDestroyMessage(process: Process): Unit = {
    context.system.scheduler.scheduleOnce(5 seconds) {
      process.destroy()
    }
  }

}

