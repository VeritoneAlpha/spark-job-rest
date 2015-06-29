package api.responses

import api.types.ID
import api.entities.ContextState.ContextState
import api.json.JsonProtocol._


case class Context(contextName: String, contextId: ID, state: ContextState, sparkUiPort: String)

object Context {
  implicit val logJson = jsonFormat4(apply)
}

case class Contexts(contexts: Array[Context])

object Contexts {
  implicit val logJson = jsonFormat1(apply)
}

case class Job(jobId: String, contextName: String, status: String, result: String, startTime: Long)

object Job {
  implicit val logJson = jsonFormat5(apply)
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





