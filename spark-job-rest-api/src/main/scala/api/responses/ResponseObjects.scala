package api.responses

import api.entities.ContextState.ContextState
import api.entities.JobState.JobState
import api.entities.{ContextDetails, JobDetails}
import api.json.JsonProtocol._
import api.types.ID
import org.joda.time.DateTime


case class Context(contextName: String, contextId: ID, state: ContextState, sparkUiPort: String)

object Context {
  implicit val logJson = jsonFormat4(apply)

  /**
   * Converts context details to it's brief representation of context response
   * @param contextDetails full context info
   * @return brief context info
   */
  def fromContextDetails(contextDetails: ContextDetails): Context = contextDetails match {
    case ContextDetails(contextName, _, _, _, state, _, sparkUiPort, contextId) =>
      Context(contextName, contextId, state, sparkUiPort.getOrElse(""))
  }
}

case class Contexts(contexts: Array[Context])

object Contexts {
  implicit val logJson = jsonFormat1(apply)
}

case class Job(jobId: ID,
               contextName: Option[String],
               contextId: Option[ID],
               status: JobState,
               result: Option[String],
               startTime: Option[String],
               duration: Option[Double])

object Job {
  implicit val logJson = jsonFormat7(apply)

  /**
   * Converts job details to job response stripping implementation details and debug info
   * @param details full job info
   * @return job response
   */
  def fromJobDetails(details: JobDetails): Job = {
    val startTime = details.startTime match {
      case Some(timestamp) => Some(new DateTime(timestamp).toLocalDateTime.toString)
      case _ => None
    }
    val duration = (details.startTime, details.stopTime) match {
      case (Some(start), Some(stop)) => Some((stop - start).toDouble / 1000)
      case _ => None
    }
    Job(details.id, details.contextName, details.contextId, details.status, details.result, startTime, duration)
  }
}

case class Jobs(jobs: Array[Job])

object Jobs {
  implicit val logJson = jsonFormat1(apply)
}

case class JarInfo(name: String, size: Long, timestamp: Long)

object JarInfo {
  implicit val logJson = jsonFormat3(apply)
}

case class JarsInfo(jars: Array[JarInfo])

object JarsInfo {
  implicit val logJson = jsonFormat1(apply)
}

case class ErrorResponse(error: String)

object ErrorResponse {
  implicit val logJson = jsonFormat1(apply)
}

case class SimpleMessage(message: String)

object SimpleMessage {
  implicit val logJson = jsonFormat1(apply)
}





