package server.domain.actors

import java.io.File
import java.util

import akka.actor.{Actor, ActorRef, ActorSelection, Props}
import akka.pattern.ask
import api.entities.ContextState.Running
import api.entities.{ContextDetails, ContextState, Jars}
import api.responses.{Context, Contexts}
import api.types._
import com.typesafe.config.{Config, ConfigFactory}
import config.durations
import org.apache.commons.lang.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import persistence.services.ContextPersistenceService._
import persistence.slickWrapper.Driver.api._
import server.domain.actors.ContextManagerActor._
import server.domain.actors.JarActor.{GetJarsPathForAll, ResultJarsPathForAll}
import server.domain.actors.messages.IsAwake
import utils.ActorUtils
import utils.DatabaseUtils.dbConnection

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process.{Process, ProcessBuilder}
import scala.util.{Failure, Success}

/**
 * Context management messages
 */
object ContextManagerActor {
  case class CreateContext(contextName: String, jars: String, config: Config)
  case class ContextInitialized(port: String)
  case class DeleteContext(contextName: String)
  case class ContextProcessTerminated(contextName: String, statusCode: Int)
  case class GetContext(contextName: String)
  case class GetContextInfo(contextName: String)
  case object GetAllContextsForClient
  case object GetAllContexts
  case object NoSuchContext
  case object ContextAlreadyExists
  case class DestroyProcess(process: Process)
  case class ContextInfo(contextName: String, contextId: ID, sparkUiPort: String, @transient referenceActor: ActorSelection)
  case class UnrecoverableContextError(error: Throwable, contextName: String, contextId: ID)
}

/**
 * Actor that creates, monitors and destroys contexts and corresponding processes.
 * @param defaultConfig configuration defaults
 * @param jarActor actor that responsible for jars which may be included to context classpath
 */
class ContextManagerActor(defaultConfig: Config, jarActor: ActorRef, connectionProviderActor: ActorRef) extends Actor {

  val log = LoggerFactory.getLogger(getClass)

  /**
   * Set config for configurable traits
   */
  val config = defaultConfig

  var lastUsedPort = getValueFromConfig(defaultConfig, "spark.job.rest.appConf.actor.systems.first.port", 11000)
  var lastUsedPortSparkUi = getValueFromConfig(defaultConfig, "spark.job.rest.appConf.spark.ui.first.port", 16000)

  val contextMap = new mutable.HashMap[String, ContextInfo]() with mutable.SynchronizedMap[String, ContextInfo]
  val processMap = new mutable.HashMap[String, ActorRef]() with mutable.SynchronizedMap[String, ActorRef]

  val sparkUIConfigPath: String = "spark.ui.port"

  /**
   * Database connection received from connection provider [[server.domain.actors.DatabaseServerActor]]
   */
  var db: Database = _

  override def preStart() = {
    db = dbConnection(connectionProviderActor)
  }

  override def receive: Receive = {
    case CreateContext(contextName, jars, contextConfig) =>
      if (contextMap contains contextName) {
        sender ! ContextAlreadyExists
      } else if (jars.isEmpty) {
        sender ! ContextActor.FailedInit("jars property is not defined or is empty.")
      } else {
        // Adding the default configs
        var mergedConfig = contextConfig.withFallback(defaultConfig)

        // The port for the actor system
        val port = ActorUtils.findAvailablePort(lastUsedPort + 1)
        lastUsedPort = port

        // If not defined, setting the spark.ui port
        if (!contextConfig.hasPath(sparkUIConfigPath)) {
          mergedConfig = addSparkUiPortToConfig(mergedConfig)
        }

        val webSender = sender()
        log.info(s"Received CreateContext message : context=$contextName jars=$jars")

        val jarsFuture = jarActor ? GetJarsPathForAll(jars, contextName)

        jarsFuture map {
          case result @ ResultJarsPathForAll(pathForClasspath, pathForSpark) =>
            log.info(s"Received jars path: $result")
            val processBuilder = createProcessBuilder(contextName, port, pathForClasspath, mergedConfig)
            val command = processBuilder.toString
            log.info(s"Starting new process for context $contextName: '$command'")
            val processActor = context.actorOf(Props(classOf[ContextProcessActor], processBuilder, contextName))
            processMap += contextName -> processActor

            val host = getValueFromConfig(defaultConfig, ActorUtils.HOST_PROPERTY_NAME, "127.0.0.1")
            val actorRef = context.actorSelection(ActorUtils.getContextActorAddress(contextName, host, port))

            // Persist context state and obtain context ID
            val contextDetails = ContextDetails(contextName, contextConfig, Some(mergedConfig), Jars.fromString(jars))

            insertContext(contextDetails, db) onComplete {
              // Initialize context if successfully inserted to database
              case Success(_) =>
                // Initialize context
                sendInitMessage(contextName, contextDetails.id, port, actorRef, webSender, mergedConfig, pathForSpark)
              // Catch persistence error
              case Failure(e: Throwable) =>
                log.error(s"Failed! ${ExceptionUtils.getStackTrace(e)}")
                webSender ! e
              case reason =>
                log.error(s"Failed! $reason")
                webSender ! ContextActor.FailedInit(s"Can't save context to database: $reason.")
            }
        } onFailure {
          case e: Throwable =>
            log.error(s"Failed! ${ExceptionUtils.getStackTrace(e)}")
            webSender ! e
        }
      }

    case DeleteContext(contextName) =>
      log.info(s"Received DeleteContext message : context=$contextName")
      if (contextMap contains contextName) {
        for (
          contextInfo <- contextMap remove contextName;
          processRef <- processMap remove contextName
        ) {
          contextInfo.referenceActor ! ContextActor.ShutDown
          sender ! Success

          // Terminate process
          processRef ! ContextProcessActor.Terminate
        }
      } else {
        sender ! NoSuchContext
      }

    case ContextProcessTerminated(contextName, statusCode) =>
      log.info(s"Received ContextProcessTerminated message : context=$contextName, statusCode=$statusCode")
      contextMap remove contextName foreach {
        case contextInfo: ContextInfo =>
          // Persist process failure
          updateContextState(contextInfo.contextId, ContextState.Failed, db, s"Context process terminated with status code $statusCode")
          // FIXME: this might be unnecessary since context process is already down
          contextInfo.referenceActor ! DeleteContext(contextName)
      }

    case GetContext(contextName) =>
      log.info(s"Received GetContext message : context=$contextName")
      if (contextMap contains contextName) {
        sender ! contextMap(contextName).referenceActor
      } else {
        sender ! NoSuchContext
      }

    case GetContextInfo(contextName) =>
      log.info(s"Received GetContext message : context=$contextName")
      if (contextMap contains contextName) {
        val ContextInfo(_, contextId, sparkUiPort, _) = contextMap(contextName)
        sender ! Context(contextName, contextId, Running, sparkUiPort)
      } else {
        sender ! NoSuchContext
      }

    case GetAllContextsForClient =>
      log.info(s"Received GetAllContexts message.")
      sender ! Contexts(contextMap.values.map {
        case ContextInfo(contextName, contextId, sparkUiPort, _) => Context(contextName, contextId, Running, sparkUiPort)
      }.toArray)

    case GetAllContexts =>
      sender ! contextMap.values.map(_.referenceActor)
      log.info(s"Received GetAllContexts message.")

    case UnrecoverableContextError(error, contextName, contextId) =>
      log.error(s"Unrecoverable error on context $contextName : $contextId: $error")
  }

  def sendInitMessage(contextName: String, contextId: ID, port: Int, actorRef: ActorSelection, sender: ActorRef, config: Config, jarsForSpark: List[String]): Unit = {

    val sleepTime = durations.context.sleep
    val tries = durations.context.tries
    val retryTimeOut = durations.context.timeout.duration
    val retryInterval = durations.context.interval
    val sparkUiPort = config.getString(sparkUIConfigPath)

    context.system.scheduler.scheduleOnce(sleepTime) {
      val isAwakeFuture = context.actorOf(ReTry.props(tries, retryTimeOut, retryInterval, actorRef)) ? IsAwake
      isAwakeFuture map {
        case isAwake =>
          log.info(s"Remote context actor is awaken: $isAwake")
          val initializationFuture = actorRef ? ContextActor.Initialize(contextName, contextId, connectionProviderActor, config, jarsForSpark)
          initializationFuture map {
            case ContextActor.Initialized =>
              log.info(s"Context '$contextName' initialized")
              contextMap += contextName -> ContextInfo(contextName, contextId, sparkUiPort, actorRef)
              setContextSparkUiPort(contextId, sparkUiPort, db)
              sender ! Context(contextName, contextId, Running, sparkUiPort)
            case error @ ContextActor.FailedInit(reason) =>
              log.error(s"Init failed for context $contextName", reason)
              sender ! error
              processMap.remove(contextName).get ! ContextProcessActor.Terminate
          } onFailure {
            case e: Exception =>
              updateContextState(contextId, ContextState.Failed, db, "Failed to send init message")
              log.error("FAILED to send init message!", e)
              sender ! ContextActor.FailedInit(ExceptionUtils.getStackTrace(e))
              processMap.remove(contextName).get ! ContextProcessActor.Terminate
          }
      } onFailure {
        case e: Exception =>
          updateContextState(contextId, ContextState.Failed, db, s"Context creation timeout: $e")
          log.error("Refused to wait for remote actor, consider it as dead!", e)
          sender ! ContextActor.FailedInit(ExceptionUtils.getStackTrace(e))
      }
    }
  }

  def addSparkUiPortToConfig(config: Config): Config = {
    lastUsedPortSparkUi = ActorUtils.findAvailablePort(lastUsedPortSparkUi + 1)
    val map = new util.HashMap[String, String]()
    map.put(sparkUIConfigPath, lastUsedPortSparkUi.toString)
    val newConf = ConfigFactory.parseMap(map)
    newConf.withFallback(config)
  }

  def createProcessBuilder(contextName: String, port: Int, jarsForClasspath: String, config: Config): ProcessBuilder = {
    val scriptPath = new File(System.getenv("SPARK_JOB_REST_CONTEXT_START_SCRIPT")).getPath
    val xmxMemory = getValueFromConfig(config, "driver.xmxMemory", "1g")

    // Create context process directory
    val processDirName = new java.io.File(defaultConfig.getString("spark.job.rest.context.contexts-base-dir")).toString + s"/$contextName"

    Process(scriptPath, Seq(jarsForClasspath, contextName, port.toString, xmxMemory, processDirName))
  }
}

