package server.domain.actors

import java.net.URL

import scala.collection.mutable.{SynchronizedMap, HashMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging, Terminated}
import com.typesafe.config.Config
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.log4j.Logger
import org.apache.spark.SparkContext

import api.{SparkJobValid, SparkJobInvalid, SparkJob}
import server.domain.actors.ContextActor._
import server.domain.actors.ContextManagerActor.{DeleteContext, IsAwake}
import server.domain.actors.JobActor._
import util.ContextURLClassLoader

/**
 * Created by raduc on 04/11/14.
 */
object ContextActor {
  case class InitializeContext(contextName: String, config: Config)
  case class Initialized()
  case class FailedInit(message: String)
}

class ContextActor(localConfig: Config) extends Actor with ActorLogging{

  private val logger = Logger.getLogger(getClass)

  var sparkContext: SparkContext = _
  var defaultConfig: Config = _
  var jobStateMap = new HashMap[String, JobStatus]() with SynchronizedMap[String, JobStatus]

  startWatchingManagerActor()

  override def receive: Receive = {

    case IsAwake => {
      sender ! IsAwake
    }

    case InitializeContext(contextName, config) => {
      logger.info(s"Received InitializeContext message : contextName=$contextName")
      // log.info("Initializing context " + contextName)

      try {

        defaultConfig = config
        val sparkConf = configToSparkConf(config,contextName)
        sparkContext = new SparkContext(sparkConf)

        sender ! Initialized
        // log.info("Successfully initialized context " + contextName)
      } catch {
        case e:Exception => {
          e.printStackTrace()
          sender ! FailedInit(ExceptionUtils.getStackTrace(e))

          gracefullyShutdown
        }
      }
    }

    case DeleteContext(contextName) => {
      logger.info(s"Received DeleteContext message : contextName=$contextName")
      // log.info("Shutting down SparkContext {}", contextName)

      gracefullyShutdown
    }

    case job:RunJob => {
      logger.info(s"Received RunJob message : runningClass=${job.runningClass} jars=${job.jars} contextName=${job.contextName} uuid=${job.uuid} ")
      jobStateMap += (job.uuid -> JobStarted())

      val jarLoader =  new ContextURLClassLoader(Array[URL](), Thread.currentThread.getContextClassLoader)

      Future {
        for (jar <- job.jars.split(":")) {
          sparkContext.addJar(jar) // add jars for remote executors
          jarLoader.addURL(new URL("file:" + jar)) // add jar to current thread class loader for driver
        }

        val runnableClass = jarLoader.loadClass(job.runningClass)
        val sparkJob = runnableClass.newInstance.asInstanceOf[SparkJob]

        val status = sparkJob.validate(sparkContext, job.config.withFallback(defaultConfig))
        status match {
          case SparkJobInvalid(message) => throw new Exception(message)
          case SparkJobValid() => logger.info("Validation passed.")
        }

        sparkJob.runJob(sparkContext, job.config.withFallback(defaultConfig))
      } andThen {
        case Success(result) => {
          logger.info(s"Finished running job : runningClass=${job.runningClass} jars=${job.jars} contextName=${job.contextName} uuid=${job.uuid} ")
          jobStateMap += (job.uuid -> JobRunSuccess(result))
          jarLoader.close()
        }
        case Failure(e:Throwable) => {
          e.printStackTrace()
          jobStateMap += (job.uuid -> JobRunError(ExceptionUtils.getStackTrace(e)))
          logger.info(s"Error running job : runningClass=${job.runningClass} jars=${job.jars} contextName=${job.contextName} uuid=${job.uuid} ")
          jarLoader.close()
        }
      }
    }
    case Terminated(actor) => {
      if(actor.path.toString.contains("Supervisor/ContextManager")){
        logger.info(s"Received TERMINATED message from: ${actor.path.toString}")
        logger.info("Shutting down the system because the ManagerSystem terminated.")
        gracefullyShutdown
      }
    }

    case JobStatusEnquiry(contextName, jobId) => {
      sender ! jobStateMap.getOrElse(jobId, JobDoesNotExist())
    }

    case x @ _ => {
      logger.info(s"Received UNKNOWN message type $x")
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
        logger.info(s"Now watching the ContextManager from this actor.")
        context.watch(actorRef)
      }
      case x @ _ => logger.info(s"Received message of type $x")
    }
  }
}
