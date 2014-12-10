package server.domain.actors

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.pattern.ask
import com.typesafe.config.Config
import org.apache.commons.lang.exception.ExceptionUtils
import server.domain.actors.ContextManagerActor.{GetContext, NoSuchContext}
import server.domain.actors.JobActor._

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by raduc on 03/11/14.
 */


object JobActor {

  trait JobStatus

  case class JobStatusEnquiry(uuid: String)

  case class RunJob(runningClass: String, contextName: String, config: Config, uuid: String = UUID.randomUUID().toString)

  case class JobRunError(errorMessage: String) extends JobStatus

  case class JobRunSuccess() extends JobStatus

  case class JobStarted() extends JobStatus

  case class JobDoesNotExist() extends JobStatus

  case class UpdateJobStatus(uuid: String, status: JobStatus)

}


class JobActor(config: Config, contextManagerActor: ActorRef) extends Actor with ActorLogging {
 var jobStateMap = new HashMap[String, JobStatus]() with SynchronizedMap[String, JobStatus]

  override def receive: Receive = {
    case job: RunJob => {
      println(s"Received RunJob message : runningClass=${job.runningClass} context=${job.contextName} uuid=${job.uuid}")

      val fromWebApi = sender

      val future = contextManagerActor ? GetContext(job.contextName)
      future.onSuccess {
        case contextRef: ActorSelection => {

          jobStateMap += (job.uuid -> JobStarted())
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
    case JobStatusEnquiry(uuid) => {
      println(s"Received JobStatusEnquiry message : uuid=$uuid")
      val state = jobStateMap.getOrElse(uuid, JobDoesNotExist)
      println("Job with id: " + uuid + "  has state : " + state)
      sender ! state
    }

    case UpdateJobStatus(uuid, status) => {
      jobStateMap += (uuid -> status)
      println(s"Job $uuid finished with status $status")
    }
  }
}


