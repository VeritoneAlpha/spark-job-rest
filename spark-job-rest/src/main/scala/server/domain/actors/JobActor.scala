package server.domain.actors

import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSelection}
import akka.pattern.ask
import com.typesafe.config.Config
import server.domain.actors.ContextManagerActor.{GetAllContexts, GetContext, NoSuchContext}
import org.slf4j.LoggerFactory
import server.domain.actors.JobActor._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Failure}

/**
 * Created by raduc on 03/11/14.
 */


object JobActor {

  trait JobStatus

  case class JobStatusEnquiry(contextName: String, jobId: String)

  case class RunJob(runningClass: String, contextName: String, config: Config, uuid: String = UUID.randomUUID().toString)

  case class JobRunError(errorMessage: String) extends JobStatus

  case class JobRunSuccess(result:String) extends JobStatus

  case class JobStarted() extends JobStatus

  case class JobDoesNotExist() extends JobStatus

  case class UpdateJobStatus(uuid: String, status: JobStatus)

  case class GetAllJobsStatus()

}


class JobActor(config: Config, contextManagerActor: ActorRef) extends Actor {

  val log = LoggerFactory.getLogger(getClass)

  override def receive: Receive = {
    case job: RunJob => {
      log.info(s"Received RunJob message : runningClass=${job.runningClass} context=${job.contextName} uuid=${job.uuid}")

      val fromWebApi = sender

      val future = contextManagerActor ? GetContext(job.contextName)
      future onSuccess {
        case contextRef: ActorSelection => {

          fromWebApi ! job.uuid

          log.info(s"Sending RunJob message to actor $contextRef")
          contextRef ! job
        }
        case NoSuchContext => fromWebApi ! NoSuchContext
        case e @ _ => log.warn(s"Received UNKNOWN TYPE when asked for context. Type received $e")
      }
      future onFailure {
        case e => {
          fromWebApi ! e
          log.error(s"An error has occured.", e)
        }
      }
    }


    case jobEnquiry:JobStatusEnquiry => {
      log.info(s"Received JobStatusEnquiry message : uuid=${jobEnquiry.jobId}")
      val fromWebApi = sender


      val contextActorFuture = contextManagerActor ? GetContext(jobEnquiry.contextName)

      contextActorFuture onSuccess {
        case contextRef: ActorSelection => {

          val enquiryFuture = contextRef ? jobEnquiry

          enquiryFuture onSuccess{
            case state:JobStatus => {
              log.info("Job with id: " + jobEnquiry.jobId + "  has state : " + state)
              fromWebApi ! state
            }
            case x:Any => {
              log.info(s"Received $x TYPE when asked for job enquiry.")
              fromWebApi ! x
            }
          }

          enquiryFuture onFailure {
            case e => {
              fromWebApi ! e
              log.error(s"An error has occured.", e)
            }
          }
        }
        case NoSuchContext => fromWebApi ! NoSuchContext
        case e @ _ => log.warn(s"Received UNKNOWN TYPE when asked for context. Type received $e")
      }

      contextActorFuture onFailure {
        case e => {
          fromWebApi ! e
          log.error(s"An error has occured.", e)
        }
      }
    }

    case GetAllJobsStatus() => {

      val webApi = sender
      val future = contextManagerActor ? GetAllContexts()

      val future2: Future[Future[List[List[Any]]]] = future map {
        case contexts: List[ActorSelection] => {
          val contextsList = contexts.map { context =>
            val oneContextFuture = context ? GetAllJobsStatus()
            oneContextFuture.map{
              case jobs: List[Any] => jobs
            }
          }
          Future.sequence(contextsList)
        }
      }
      val future3: Future[List[List[Any]]] = future2.flatMap(identity)
      val future4: Future[List[Any]] = future3.map(x => x.flatMap(identity))

      future4 onComplete {
        case Success(s) => webApi ! s
        case Failure(e) => webApi ! e
      }

    }
  }
}


