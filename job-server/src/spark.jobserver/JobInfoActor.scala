package spark.jobserver

import akka.actor.{ActorSelection, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import ooyala.common.akka.InstrumentedActor
import scala.concurrent.Await
import spark.jobserver.ContextSupervisor.{GetContext, GetAdHocContext}
import spark.jobserver.io._
import java.util.concurrent.TimeUnit
import spark.jobserver.io.GetJobInfos
import spark.jobserver.io.JobInfo
import spark.jobserver.io.SaveJobConfig


object JobInfoActor {
  // Requests
  case class GetJobStatuses(limit: Option[Int])
  case class GetJobConfig(jobId: String)
  case class StoreJobConfig(jobId: String, jobConfig: Config)

  // Responses
  case object JobConfigStored
}

class JobInfoActor(jobDaoRef: ActorRef, contextSupervisor: ActorRef) extends InstrumentedActor {
  import CommonMessages._
  import JobInfoActor._
  import scala.concurrent.duration._
  import scala.util.control.Breaks._
  import context.dispatcher

  // Used in the asks (?) below to request info from contextSupervisor and resultActor
  implicit val ShortTimeout = Timeout(5, TimeUnit.SECONDS)

  override def wrappedReceive: Receive = {
    case GetJobStatuses(limit) =>

      val future = jobDaoRef ? GetJobInfos()
      val result = Await.result(future, 5 seconds).asInstanceOf[Map[String, JobInfo]]

      val infos = result.values.toSeq.sortBy(_.startTime.toString())
      if (limit.isDefined) {
        sender ! infos.takeRight(limit.get)
      } else {
        sender ! infos
      }

    case GetJobResult(jobId) =>
      breakable {

        val futureGetJobs = jobDaoRef ? GetJobInfos()
        val result = Await.result(futureGetJobs, 5 seconds).asInstanceOf[Map[String, JobInfo]]

        val jobInfoOpt = result.get(jobId)
        if (!jobInfoOpt.isDefined) {
          sender ! NoSuchJobId
          break
        }

        jobInfoOpt.filter { job => job.isRunning || job.isErroredOut }
          .foreach { jobInfo =>
            sender ! jobInfo
            break
          }

        // get the context from jobInfo
        val context = jobInfoOpt.get.contextName

        val future = (contextSupervisor ? ContextSupervisor.GetResultActor(context)).mapTo[ActorSelection]
        val resultActor = Await.result(future, 10 seconds)


        val receiver = sender // must capture the sender since callbacks are run in a different thread
        for (result <- (resultActor ? GetJobResult(jobId))) {
          receiver ! result // a JobResult(jobId, result) object is sent
        }
      }

    case GetJobConfig(jobId) => {
      val futureGetJobs = jobDaoRef ? GetJobConfigs()
      val result = Await.result(futureGetJobs, 5 seconds).asInstanceOf[Map[String, Config]]
      sender ! result.get(jobId).getOrElse(NoSuchJobId)
    }


    case StoreJobConfig(jobId, jobConfig) =>
      jobDaoRef ! SaveJobConfig(jobId, jobConfig)
      sender ! JobConfigStored
  }
}
