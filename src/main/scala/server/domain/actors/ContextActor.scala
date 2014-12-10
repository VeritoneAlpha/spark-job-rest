package server.domain.actors

import akka.actor.{Actor, ActorLogging, Terminated}
import api.{SparkJobValid, SparkJobInvalid, SparkJob}
import com.typesafe.config.Config
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.spark.SparkContext
import ContextManagerActor.{DeleteContext, IsAwake}
import JobActor.{JobRunError, JobRunSuccess, RunJob, UpdateJobStatus}
import server.domain.actors.ContextActor._

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

class ContextActor(jarsPath: Array[String], localConfig: Config) extends Actor with ActorLogging{

  var sparkContext: SparkContext = _
  var defaultConfig: Config = _

  startWatchingManagerActor()

  override def receive: Receive = {

    case IsAwake => {
      sender ! IsAwake
    }

    case InitializeContext(contextName, config) => {

      println(s"Received InitializeContext message : contextName=$contextName")
      log.info("Initializing context " + contextName)

      try {

        defaultConfig = config
        val sparkConf = configToSparkConf(config,contextName, jarsPath)
        sparkContext = new SparkContext(sparkConf)

        sender ! Initialized
        log.info("Successfully initialized context " + contextName)
      } catch {
        case e:Exception => {
          e.printStackTrace()
          sender ! FailedInit(ExceptionUtils.getStackTrace(e))

          gracefullyShutdown
        }
      }
    }

    case DeleteContext(contextName) => {
      println(s"Received DeleteContext message : contextName=$contextName")
      log.info("Shutting down SparkContext {}", contextName)

      gracefullyShutdown
    }

    case job:RunJob => {
      println(s"Received RunJob message : runningClass=${job.runningClass} contextName=${job.contextName} uuid=${job.uuid} ")

      val from = sender

      Future {
          val classLoader = Thread.currentThread.getContextClassLoader
          val runnableClass = classLoader.loadClass(job.runningClass)
          val sparkJob = runnableClass.newInstance.asInstanceOf[SparkJob]

          val status = sparkJob.validate(sparkContext, job.config.withFallback(defaultConfig))
          status match {
            case SparkJobInvalid(message) => throw new Exception(message)
            case SparkJobValid() => println("Validation passed.")
          }

          sparkJob.runJob(sparkContext, job.config.withFallback(defaultConfig))

      } andThen {
        case Success(result) => {
          println(s"Finished running job : runningClass=${job.runningClass} contextName=${job.contextName} uuid=${job.uuid} ")
          println(s"Sending message JS to actor $from")
          from ! UpdateJobStatus(job.uuid, JobRunSuccess())
        }
        case Failure(e:Throwable) => {
          e.printStackTrace()
          from ! UpdateJobStatus(job.uuid, JobRunError(ExceptionUtils.getStackTrace(e)))
          println(s"Error running job : runningClass=${job.runningClass} contextName=${job.contextName} uuid=${job.uuid} ")
        }
      }
    }
    case Terminated(actor) => {
      if(actor.path.toString.contains("Supervisor/ContextManager")){
        println(s"Received TERMINATED message from: ${actor.path.toString}")
        println("Shutting down the system because the ManagerSystem terminated.")
        gracefullyShutdown
      }
    }
    case x @ _ => {
      println(s"Received UNKNOWN message type $x")
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
        println(s"Now watching the ContextManager from this actor.")
        context.watch(actorRef)
      }
      case x @ _ => println(s"Received message of type $x")
    }
  }
}


