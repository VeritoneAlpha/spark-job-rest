package server.domain.actors

import akka.actor.{Actor, ActorLogging, Terminated}
import api.{SparkJobValid, SparkJobInvalid, SparkJob}
import com.typesafe.config.Config
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.spark.SparkContext
import ContextManagerActor.{DeleteContext, IsAwake}
import org.slf4j.LoggerFactory
import server.domain.actors.JobActor._
import server.domain.actors.ContextActor._

import scala.collection.mutable.{SynchronizedMap, HashMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Created by raduc on 04/11/14.
 */

object ContextActor {
  case class InitializeContext(contextName: String, config: Config)
  case class Initialized()
  case class FailedInit(message: String)
}

class ContextActor(jarsPath: Array[String], localConfig: Config) extends Actor {

  var sparkContext: SparkContext = _
  var defaultConfig: Config = _
  var jobStateMap = new HashMap[String, JobStatus]() with SynchronizedMap[String, JobStatus]

  val log = LoggerFactory.getLogger(getClass)

  startWatchingManagerActor()

  override def receive: Receive = {

    case IsAwake => {
      sender ! IsAwake
    }

    case InitializeContext(contextName, config) => {

      log.info(s"Received InitializeContext message : contextName=$contextName")
      log.info("Initializing context " + contextName)

      try {

        defaultConfig = config
        val sparkConf = configToSparkConf(config,contextName, jarsPath)
        sparkContext = new SparkContext(sparkConf)

        sender ! Initialized
        log.info("Successfully initialized context " + contextName)
      } catch {
        case e:Exception => {
          log.error("Exception while initializing", e)
          sender ! FailedInit(ExceptionUtils.getStackTrace(e))
          gracefullyShutdown
        }
      }
    }

    case DeleteContext(contextName) => {
      log.info(s"Received DeleteContext message : contextName=$contextName")
      log.info("Shutting down SparkContext {}", contextName)

      gracefullyShutdown
    }

    case job:RunJob => {
      log.info(s"Received RunJob message : runningClass=${job.runningClass} contextName=${job.contextName} uuid=${job.uuid} ")
      jobStateMap += (job.uuid -> JobStarted())

      Future {
          val classLoader = Thread.currentThread.getContextClassLoader
          val runnableClass = classLoader.loadClass(job.runningClass)
          val sparkJob = runnableClass.newInstance.asInstanceOf[SparkJob]

          val status = sparkJob.validate(sparkContext, job.config.withFallback(defaultConfig))
          status match {
            case SparkJobInvalid(message) => throw new Exception(message)
            case SparkJobValid() => log.info("Validation passed.")
          }

          sparkJob.runJob(sparkContext, job.config.withFallback(defaultConfig))

      } andThen {
        case Success(result) => {
          log.info(s"Finished running job : runningClass=${job.runningClass} contextName=${job.contextName} uuid=${job.uuid} ")
          jobStateMap += (job.uuid -> JobRunSuccess(result))
        }
        case Failure(e:Throwable) => {
          jobStateMap += (job.uuid -> JobRunError(ExceptionUtils.getStackTrace(e)))
          log.error(s"Error running job : runningClass=${job.runningClass} contextName=${job.contextName} uuid=${job.uuid} ", e)
        }
      }
    }
    case Terminated(actor) => {
      if(actor.path.toString.contains("Supervisor/ContextManager")){
        log.info(s"Received TERMINATED message from: ${actor.path.toString}")
        log.warn("Shutting down the system because the ManagerSystem terminated.")
        gracefullyShutdown
      }
    }

    case JobStatusEnquiry(contextName, jobId) => {
      sender ! jobStateMap.getOrElse(jobId, JobDoesNotExist())
    }

    case x @ _ => {
      log.info(s"Received UNKNOWN message type $x")
    }
  }

  def gracefullyShutdown {
    Option(sparkContext).foreach(_.stop())
    context.system.shutdown()
  }

  def startWatchingManagerActor() = {
    val managerActor = context.actorSelection(Util.getActorAddress("ManagerSystem", getValueFromConfig(localConfig,"manager.akka.remote.netty.tcp.port", 4042), "Supervisor/ContextManager"))
    managerActor.resolveOne().onComplete {
      case Success(actorRef) => {
        log.info(s"Now watching the ContextManager from this actor.")
        context.watch(actorRef)
      }
      case x @ _ => log.info(s"Received message of type $x")
    }
  }
}


