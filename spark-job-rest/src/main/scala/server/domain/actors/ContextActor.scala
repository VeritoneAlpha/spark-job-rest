package server.domain.actors

import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import api._
import api.entities.{JobState, ContextState}
import com.google.gson.Gson
import com.typesafe.config.{Config, ConfigValueFactory}
import context.JobContextFactory
import org.apache.commons.lang.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import api.types.ID
import persistence.services.ContextPersistenceService.updateContextState
import persistence.slickWrapper.Driver.api._
import api.responses.Job
import server.domain.actors.ContextActor._
import server.domain.actors.JobActor._
import utils.ActorUtils
import utils.DatabaseUtils.dbConnection

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * Context actor messages
 */
object ContextActor {
  case class Initialize(contextName: String, contextId: ID, connectionProviderActor: ActorRef, config: Config, jarsForSpark: List[String])
  case class Initialized()
  case class FailedInit(message: String)
  case class ShutDown()
}

/**
 * Context actor responsible for creation and managing Spark Context
 * @param localConfig config of the context application
 */
class ContextActor(localConfig: Config) extends Actor with Stash {
  import context.become

  val log = LoggerFactory.getLogger(getClass)

  /**
   * Spark job context which may be either directly [[org.apache.spark.SparkContext]]
   * or anything else (normally SQL context) based on top of it.
   */
  var jobContext: ContextLike = _

  /**
   * Config which will be used as a defaults for jobs running on this context
   */
  var defaultConfig: Config = _

  /**
   * Hash for tracking jobs which running on the context
   */
  var jobStateMap = new mutable.HashMap[String, JobStatus]() with mutable.SynchronizedMap[String, JobStatus]

  /**
   * Spark application name
   */
  var contextName: String = _

  /**
   * Persistent identifier of the context
   */
  var contextId: ID = _

  /**
   * Provider for database connection: [[DatabaseConnectionActor]]
   */
  var connectionProvider: ActorRef = _

  /**
   * Database connection
   */
  var db: Database = _

  /**
   * JSON serializer for job results
   */
  val gsonTransformer = new Gson()

  startWatchingManagerActor()

  /**
   * Context cleanup
   */
  override def postStop(): Unit = {
    updateContextState(contextId, ContextState.Terminated, db, "Context actor stopped")
  }

  /**
   * Initial actor mode when it responds to IsAwake message and can be initialized
   * @return
   */
  override def receive: Receive = {
    case ContextManagerActor.IsAwake =>
      sender ! ContextManagerActor.IsAwake

    case Initialize(name, id, remoteConnectionProvider, config, jarsForSpark) =>
      // Stash all messages to process them later
      stash()
      
      contextName = name
      contextId = id

      log.info(s"Received InitializeContext message : contextName=$contextName")
      log.info("Initializing context " + contextName)

      // Request connection parameters and establish connection to database
      connectionProvider = context.actorOf(Props(new DatabaseConnectionActor(remoteConnectionProvider)))
      db = dbConnection(connectionProvider)
      log.info(s"Obtained connection to database: $db")

      try {
        defaultConfig = config.withValue("context.jars", ConfigValueFactory.fromAnyRef(jarsForSpark.asJava))
        jobContext = JobContextFactory.makeContext(defaultConfig, contextName)
        updateContextState(contextId, ContextState.Running, db, s"Created job context: $jobContext")
        sender ! Initialized()
        log.info("Successfully initialized context " + contextName)
      } catch {
        case e: Exception =>
          log.error("Exception while initializing", e)
          updateContextState(contextId, ContextState.Failed, db, s"Context creation exception: $e")
          sender ! FailedInit(ExceptionUtils.getStackTrace(e))
          gracefullyShutdown()
      }

      // Switch to initialised mode and process all messages
      become(initialized)
      unstashAll()
  }

  /**
   * Main actor mode when it can run jobs
   * @return
   */
  def initialized: Receive = {
    case ShutDown() =>
      updateContextState(contextId, ContextState.Terminated, db, "Received shutdown request")
      log.info(s"Context received ShutDown message : contextName=$contextName")
      log.info(s"Shutting down SparkContext $contextName")

      gracefullyShutdown()

    case RunJob(runningClass, _, jobConfig, uuid) =>
      log.info(s"Received RunJob message : runningClass=$runningClass contextName=$contextName uuid=$uuid ")
      jobStateMap += (uuid -> JobStarted())

      Future {
        Try {
          val classLoader = Thread.currentThread.getContextClassLoader
          val runnableClass = classLoader.loadClass(runningClass)
          val sparkJob = runnableClass.newInstance.asInstanceOf[SparkJobBase]

          jobContext.validateJob(sparkJob) match {
            case SparkJobValid() => log.info(s"Job $uuid passed context validation.")
            case SparkJobInvalid(message) => throw new IllegalArgumentException(s"Invalid job $uuid: $message")
          }

          val jobConfigValidation = sparkJob.validate(jobContext.asInstanceOf[sparkJob.C], jobConfig.withFallback(defaultConfig))
          jobConfigValidation match {
            case SparkJobInvalid(message) => throw new IllegalArgumentException(message)
            case SparkJobValid() => log.info("Job config validation passed.")
          }

          sparkJob.runJob(jobContext.asInstanceOf[sparkJob.C], jobConfig.withFallback(defaultConfig))
        }
      } andThen {
        case Success(futureResult) => futureResult match {
          case Success(result) =>
            log.info(s"Finished running job : runningClass=$runningClass contextName=$contextName uuid=$uuid ")
            jobStateMap += (uuid -> JobRunSuccess(gsonTransformer.toJson(result)))
          case Failure(e: Throwable) =>
            jobStateMap += (uuid -> JobRunError(ExceptionUtils.getStackTrace(e)))
            log.error(s"Error running job : runningClass=$runningClass contextName=$contextName uuid=$uuid ", e)
          case x: Any =>
            log.error("Received ANY from running job !!! " + x)
        }

        case Failure(e: Throwable) =>
          jobStateMap += (uuid -> JobRunError(ExceptionUtils.getStackTrace(e)))
          log.error(s"Error running job : runningClass=$runningClass contextName=$contextName uuid=$uuid ", e)

        case x: Any =>
          log.error("Received ANY from running job !!! " + x)
      }

    case Terminated(actor) =>
      if (actor.path.toString.contains("Supervisor/ContextManager")) {
        updateContextState(contextId, ContextState.Terminated, db, "Stopped due to ManagerSystem termination")
        log.info(s"Received Terminated message from: ${actor.path.toString}")
        log.warn("Shutting down the system because the ManagerSystem terminated.")
        gracefullyShutdown()
      }

    case JobStatusEnquiry(_, jobId) =>
      val jobState = jobStateMap.getOrElse(jobId, JobDoesNotExist())
      import JobState._
      jobState match {
        case x: JobRunSuccess => sender ! Job(jobId, contextName, Finished.toString, x.result, x.startTime)
        case e: JobRunError => sender ! Job(jobId, contextName, Failed.toString, e.errorMessage, e.startTime)
        case x: JobStarted => sender ! Job(jobId, contextName, Running.toString, "", x.startTime)
        case x: JobDoesNotExist => sender ! JobDoesNotExist
      }

    case GetAllJobsStatus() =>
      import JobState._
      val jobsList = jobStateMap.map {
         case (id: String, x: JobRunSuccess) => Job(id, contextName, Finished.toString, x.result, x.startTime)
         case (id: String, e: JobRunError) => Job(id, contextName, Failed.toString, e.errorMessage, e.startTime)
         case (id: String, x: JobStarted) => Job(id, contextName, Running.toString, "", x.startTime)
       }.toList
      sender ! jobsList

    case x @ _ =>
      log.info(s"Received UNKNOWN message type $x")
  }

  def gracefullyShutdown() {
    Option(jobContext).foreach(_.stop())
    context.system.shutdown()
  }

  def startWatchingManagerActor() = {
    val managerPort = getValueFromConfig(localConfig, ActorUtils.PORT_PROPERTY_NAME, 4042)
    val managerHost = getValueFromConfig(localConfig, ActorUtils.HOST_PROPERTY_NAME, "127.0.0.1")
    log.info("Trying to watch the manager actor at : " + managerHost + ":" + managerPort)
    val managerActor = context.actorSelection(ActorUtils.getActorAddress("ManagerSystem", managerHost, managerPort, "Supervisor/ContextManager"))
    managerActor.resolveOne().onComplete {
      case Success(actorRef) =>
        log.info(s"Now watching the ContextManager from this actor.")
        context.watch(actorRef)
      case x @ _ => log.info(s"Received message of type $x")
    }
  }
}


