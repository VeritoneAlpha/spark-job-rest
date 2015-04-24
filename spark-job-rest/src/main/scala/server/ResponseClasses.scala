package server

import spray.json.DefaultJsonProtocol._


/**
 * Created by raduc on 24/04/15.
 */

  case class Context(contextName: String, sparkUiPort: String)

  object Context {
    implicit val logJson = jsonFormat2(apply)
  }

  case class Contexts(contexts: Array[Context])

  object Contexts {
    implicit val logJson = jsonFormat1(apply)
  }

  case class Job(jobId: String, contextName: String, status: String, result: String)

  object Job {
    implicit val logJson = jsonFormat4(apply)
  }

  case class Jobs(jobs: Array[Job])

  object Jobs {
    implicit val logJson = jsonFormat1(apply)
  }

  case class JarInfo(name: String, size: Long, timestamp: Long)

  object JarInfo {
    implicit val logJson = jsonFormat3(apply)
  }

  case class JarsInfo(jarsInfo: Array[JarInfo])

  object JarsInfo {
    implicit val logJson = jsonFormat1(apply)
  }

  case class ErrorResponse(error: String)

  object ErrorResponse {
    implicit val logJson = jsonFormat1(apply)
  }

  case class SimpleMessage(status: String)

  object SimpleMessage {
    implicit val logJson = jsonFormat1(apply)
  }





