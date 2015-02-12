package server.domain.actors

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.pattern.ask
import com.typesafe.config.Config
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.log4j.Logger

import server.domain.actors.ContextManagerActor.{GetContext, NoSuchContext}
import server.domain.actors.JarManagerActor.GetJars
import server.domain.actors.JobActor._

/**
 * Created by raduc on 03/11/14.
 */
object JobActor {

  trait JobStatus

  case class JobStatusEnquiry(contextName: String, jobId: String)
  case class RunJob(runningClass: String, jars: String, contextName: String, config: Config, uuid: String = UUID.randomUUID().toString)
  case class JobRunError(errorMessage: String) extends JobStatus
  case class JobRunSuccess(result:Any) extends JobStatus
  case class JobStarted() extends JobStatus
  case class JobDoesNotExist() extends JobStatus
  case class UpdateJobStatus(uuid: String, status: JobStatus)

}


class JobActor(config: Config, jarManagerActor: ActorRef, contextManagerActor: ActorRef) extends Actor with ActorLogging {
  private val logger = Logger.getLogger(getClass)

  override def receive: Receive = {
    case job: RunJob => {
      logger.info(s"Received RunJob message : runningClass=${job.runningClass} jars=${job.jars} context=${job.contextName} uuid=${job.uuid}")

      val fromWebApi = sender

      if (job.jars.isEmpty) {
        fromWebApi ! JobRunError("jars property is not defined or is empty.")
      } else {
        val isJarsFullPath = getValueFromConfig(job.config, "fullPath", true)
        if (!isJarsFullPath) {
          val future = jarManagerActor ? GetJars(job.jars)
          future.onSuccess {
            case fullPathJars: String => runJob(RunJob(job.runningClass, fullPathJars, job.contextName, job.config, job.uuid), fromWebApi)
            case e @ _ => logger.error(s"Get jars full path cause UNKNOW error. Infor is $e")
          }
          future.onFailure {
            case e => {
              logger.error(s"An error has occured: ${ExceptionUtils.getStackTrace(e)}")
            }
          }
        } else {
          runJob(job, fromWebApi)
        }
      }
    }


    case jobEnquiry:JobStatusEnquiry => {
      logger.info(s"Received JobStatusEnquiry message : uuid=${jobEnquiry.jobId}")
      val fromWebApi = sender

      val contextActorFuture = contextManagerActor ? GetContext(jobEnquiry.contextName)

      contextActorFuture onSuccess {
        case contextRef: ActorSelection => {

          val enquiryFuture = contextRef ? jobEnquiry

          enquiryFuture onSuccess{
            case state:JobStatus => {
              logger.info("Job with id: " + jobEnquiry.jobId + "  has state : " + state)
              fromWebApi ! state
            }
            case x:Any => {
              logger.info(s"Received $x TYPE when asked for job enquiry.")
              fromWebApi ! x
            }
          }

          enquiryFuture onFailure{
            case e => {
              fromWebApi ! e
              logger.error(s"An error has occured: ${ExceptionUtils.getStackTrace(e)}")
            }
          }
        }
        case NoSuchContext => fromWebApi ! NoSuchContext
        case e @ _ => logger.error(s"Received UNKNOWN TYPE when asked for context. Type received $e")
      }

      contextActorFuture onFailure  {
        case e => {
          fromWebApi ! e
          logger.error(s"An error has occured: ${ExceptionUtils.getStackTrace(e)}")
        }
      }
    }
  }

  def runJob(job: RunJob, fromWebApi: ActorRef){
    val future = contextManagerActor ? GetContext(job.contextName)
    future onSuccess {
      case contextRef: ActorSelection => {

        fromWebApi ! job.uuid

        logger.info(s"Sending RunJob message to actor $contextRef")
        contextRef ! job
      }
      case NoSuchContext => fromWebApi ! NoSuchContext
      case e @ _ => logger.error(s"Received UNKNOWN TYPE when asked for context. Type received $e")
    }
    future onFailure  {
      case e => {
        fromWebApi ! e
        logger.error(s"An error has occured: ${ExceptionUtils.getStackTrace(e)}")
      }
    }
  }
}


