package server.domain.actors

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.pattern.ask
import com.typesafe.config.Config
import org.apache.commons.lang.exception.ExceptionUtils
import server.domain.actors.ContextManagerActor.{GetContext, NoSuchContext}
import server.domain.actors.JobActor._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by raduc on 03/11/14.
 */


object JobActor {

  trait JobStatus

  case class JobStatusEnquiry(contextName: String, jobId: String)

  case class RunJob(runningClass: String, contextName: String, config: Config, uuid: String = UUID.randomUUID().toString)

  case class JobRunError(errorMessage: String) extends JobStatus

  case class JobRunSuccess(result:Any) extends JobStatus

  case class JobStarted() extends JobStatus

  case class JobDoesNotExist() extends JobStatus

  case class UpdateJobStatus(uuid: String, status: JobStatus)

}


class JobActor(config: Config, contextManagerActor: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case job: RunJob => {
      println(s"Received RunJob message : runningClass=${job.runningClass} context=${job.contextName} uuid=${job.uuid}")

      val fromWebApi = sender

      val future = contextManagerActor ? GetContext(job.contextName)
      future onSuccess {
        case contextRef: ActorSelection => {

          fromWebApi ! job.uuid

          println(s"Sending RunJob message to actor $contextRef")
          contextRef ! job
        }
        case NoSuchContext => fromWebApi ! NoSuchContext
        case e @ _ => println(s"Received UNKNOWN TYPE when asked for context. Type received $e")
      }
      future onFailure  {
        case e => {
          fromWebApi ! e
          println(s"An error has occured: ${ExceptionUtils.getStackTrace(e)}")
        }
      }
    }


    case jobEnquiry:JobStatusEnquiry => {
      println(s"Received JobStatusEnquiry message : uuid=${jobEnquiry.jobId}")
      val fromWebApi = sender


      val contextActorFuture = contextManagerActor ? GetContext(jobEnquiry.contextName)

      contextActorFuture onSuccess {
        case contextRef: ActorSelection => {

          val enquiryFuture = contextRef ? jobEnquiry

          enquiryFuture onSuccess{
            case state:JobStatus => {
              println("Job with id: " + jobEnquiry.jobId + "  has state : " + state)
              fromWebApi ! state
            }
            case x:Any => {
              println(s"Received $x TYPE when asked for job enquiry.")
              fromWebApi ! x
            }
          }

          enquiryFuture onFailure{
            case e => {
              fromWebApi ! e
              println(s"An error has occured: ${ExceptionUtils.getStackTrace(e)}")
            }
          }
        }
        case NoSuchContext => fromWebApi ! NoSuchContext
        case e @ _ => println(s"Received UNKNOWN TYPE when asked for context. Type received $e")
      }

      contextActorFuture onFailure  {
        case e => {
          fromWebApi ! e
          println(s"An error has occured: ${ExceptionUtils.getStackTrace(e)}")
        }
      }
    }

  }
}


