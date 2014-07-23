package spark.jobserver

import akka.actor.ActorRef
import ooyala.common.akka.InstrumentedActor
import ooyala.common.akka.metrics.YammerMetrics
import scala.collection.mutable
import spark.jobserver.util.LRUCache
import com.typesafe.config.ConfigFactory
import java.io.File

/**
 * It is an actor to manage results that are returned from jobs.
 *
 * TODO: support multiple subscribers for same JobID
 */
class JobResultActor(configFileString: String) extends InstrumentedActor with YammerMetrics {
  import CommonMessages._

//  private val config = ConfigFactory.load()
  val configFile = new File(configFileString)
  private val config = ConfigFactory.parseFile(configFile)

  private val cache = new LRUCache[String, Any](config.getInt("spark.jobserver.job-result-cache-size"))
  private val subscribers = mutable.HashMap.empty[String, ActorRef] // subscribers

  // metrics
  val metricSubscribers = gauge("subscribers-size", subscribers.size)
  val metricResultCache = gauge("result-cache-size", cache.size)

  println(self.path)

  def wrappedReceive: Receive = {
    case Subscribe(jobId, receiver, events) =>
      if (events.contains(classOf[JobResult])) {
        subscribers(jobId) = receiver
        logger.info("Added receiver {} to subscriber list for JobID {}", receiver, jobId: Any)
      }

    case Unsubscribe(jobId, receiver) =>
      if (!subscribers.contains(jobId)) {
        sender ! NoSuchJobId
      } else {
        subscribers.remove(jobId)
        logger.info("Removed subscriber list for JobID {}", jobId)
      }

    case GetJobResult(jobId) =>
      println("Received getJobResultsMessage " + jobId)
      sender ! cache.get(jobId).map(JobResult(jobId, _)).getOrElse(NoSuchJobId)

    case JobResult(jobId, result) =>
      cache.put(jobId, result)
      logger.debug("Received job results for JobID {}", jobId)
      subscribers.get(jobId).foreach(_ ! JobResult(jobId, result))
      subscribers.remove(jobId)
    case x:String => logger.info("JobResultActor received a message : " + x)
  }

}
