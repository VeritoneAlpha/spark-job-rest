package server.domain.actors

import akka.actor.{Actor, Terminated}
import api._
import com.google.gson.Gson
import com.typesafe.config.{Config, ConfigValueFactory}
import context.JobContextFactory
import org.apache.commons.lang.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import responses.{Job, JobStates}
import server.domain.actors.ContextActor._
import server.domain.actors.JobActor._
import utils.ActorUtils

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * Context actor messages
 */
object ContextActor {
  case class Initialize(contextName: String, config: Config, jarsForSpark: List[String])
  case class Initialized()
  case class FailedInit(message: String)
  case class ShutDown()
}

/**
 * Context actor responsible for creation and managing Spark Context
 * @param localConfig config of the context application
 */
class ContextActor(localConfig: Config) extends Actor {
  import context.become

  val log = LoggerFactory.getLogger(getClass)
  var jobContext: ContextLike = _
  var defaultConfig: Config = _
  var jobStateMap = new mutable.HashMap[String, JobStatus]() with mutable.SynchronizedMap[String, JobStatus]

  var name = ""
  val gsonTransformer = new Gson()

  startWatchingManagerActor()

  /**
   * Initial actor mode when it responds to IsAwake message and can be initialized
   * @return
   */
  override def receive: Receive = {
    case ContextManagerActor.IsAwake =>
      sender ! ContextManagerActor.IsAwake

    case Initialize(contextName, config, jarsForSpark) =>
      log.info(s"Received InitializeContext message : contextName=$contextName")
      log.info("Initializing context " + contextName)
      name = contextName

      try {
        defaultConfig = config.withValue("context.jars", ConfigValueFactory.fromAnyRef(jarsForSpark.asJava))
        jobContext = JobContextFactory.makeContext(defaultConfig, name)

        sender ! Initialized()
        log.info("Successfully initialized context " + contextName)
      } catch {
        case e: Exception =>
          log.error("Exception while initializing", e)
          sender ! FailedInit(ExceptionUtils.getStackTrace(e))
          gracefullyShutdown()
      }

      become(initialized)
  }

  /**
   * Main actor mode when it can run jobs
   * @return
   */
  def initialized: Receive = {
    case ShutDown() =>
      log.info(s"Context received ShutDown message : contextName=$name")
      log.info(s"Shutting down SparkContext $name")

      gracefullyShutdown()

    case RunJob(runningClass, contextName, jobConfig, uuid) =>
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
        log.info(s"Received Terminated message from: ${actor.path.toString}")
        log.warn("Shutting down the system because the ManagerSystem terminated.")
        gracefullyShutdown()
      }

    case JobStatusEnquiry(contextName, jobId) =>
      val jobState = jobStateMap.getOrElse(jobId, JobDoesNotExist())
      import JobStates._
      jobState match {
        case x: JobRunSuccess => sender ! Job(jobId, name, FINISHED.toString, x.result, x.startTime)
        case e: JobRunError => sender ! Job(jobId, name, ERROR.toString, e.errorMessage, e.startTime)
        case x: JobStarted => sender ! Job(jobId, name, RUNNING.toString, "", x.startTime)
        case x: JobDoesNotExist => sender ! JobDoesNotExist
      }

    case GetAllJobsStatus() =>
      import JobStates._
      val jobsList = jobStateMap.map {
         case (id: String, x: JobRunSuccess) => Job(id, name, FINISHED.toString, x.result, x.startTime)
         case (id: String, e: JobRunError) => Job(id, name, ERROR.toString, e.errorMessage, e.startTime)
         case (id: String, x: JobStarted) => Job(id, name, RUNNING.toString, "", x.startTime)
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


