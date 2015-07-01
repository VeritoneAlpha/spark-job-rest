package client

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import api.responses._
import api.types.ID
import org.slf4j.LoggerFactory
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller
import spray.httpx.UnsuccessfulResponseException

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
* Created by raduc on 23/04/15.
*/
class SparkJobRestClient(serverAddress: String)(implicit system: ActorSystem) {
  import system.dispatcher
  val log = LoggerFactory.getLogger(getClass)

  val contextsRoute = "/contexts"
  val jobsRoute = "/jobs"
  val jarsRoute = "/jars"
  val heartBeatRoute = "/heartbeat"

  val SEPARATOR = "/"

  implicit val timeout = Timeout(30, TimeUnit.SECONDS)


//  ============  Contexts Route  ============
  @throws(classOf[Exception])
  def getContexts: Contexts = {

    val pipeline: HttpRequest => Future[Contexts] = sendReceive ~> unmarshal[Contexts]

    val response: Future[Contexts] = pipeline(Get(serverAddress + contextsRoute))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(contexts: Contexts) =>
        return contexts
      case Failure(e) =>
        log.error(s"Failed request: $e")
        throw e
    }

    null
  }

  @throws(classOf[Exception])
  def getContext: Context = {
    val pipeline: HttpRequest => Future[Context] = sendReceive ~> unmarshal[Context]
    val response: Future[Context] = pipeline(Get(serverAddress + contextsRoute))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(context: Context) =>
        return context
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        throw e
      case Failure(e) =>
        log.error("Failed request: ", e)
        throw e
    }

    null
  }

  @throws(classOf[Exception])
  def checkIfContextExists(contextName: String) : Boolean = {
    val pipeline: HttpRequest => Future[Context] = sendReceive ~> unmarshal[Context]
    val response: Future[Context] = pipeline(Get(serverAddress + contextsRoute + SEPARATOR + contextName))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(context: Context) =>
        return true
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        return false
      case Failure(e: Throwable) =>
        log.error("Unsuccessful request: ", e)
        throw e
    }

    false
  }

  @throws(classOf[Exception])
  def deleteContext(contextName: String) : Boolean = {
    val pipeline: HttpRequest => Future[SimpleMessage] = sendReceive ~> unmarshal[SimpleMessage]
    val response: Future[SimpleMessage] = pipeline(Delete(serverAddress + contextsRoute + SEPARATOR + contextName))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(simpleMessage: SimpleMessage) =>
        return true
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        throw e
      case Failure(e: Throwable) =>
        log.error("Unsuccessful request: ", e)
        throw e
    }

    false
  }

  @throws(classOf[Exception])
  def createContext(contextName: String, parameters: Map[String, String]) : Context = {
    val body = createParametersString(parameters)
    val pipeline: HttpRequest => Future[Context] = sendReceive ~> unmarshal[Context]
    val response: Future[Context] = pipeline(Post(serverAddress + contextsRoute + SEPARATOR + contextName, body))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(context: Context) =>
        return context
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        throw e
      case Failure(e: Throwable) =>
        log.error("Unsuccessful request: ", e)
        throw e

    }

    null
  }

//  ============  Jobs Route  ============
  @throws(classOf[Exception])
  def getJobs: Jobs = {
    val pipeline: HttpRequest => Future[Jobs] = sendReceive ~> unmarshal[Jobs]
    val response: Future[Jobs] = pipeline(Get(serverAddress + jobsRoute))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(jobs: Jobs) =>
        return jobs
      case Failure(e) =>
        log.error("Failed request: ", e)
        throw e
    }

    null
  }

  @throws(classOf[Exception])
  def getJob(jobId: ID) : Job = {
    val pipeline: HttpRequest => Future[Job] = sendReceive ~> unmarshal[Job]
    val response: Future[Job] = pipeline(Get(serverAddress + jobsRoute + SEPARATOR + jobId))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(job: Job) =>
        return job
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        throw e
      case Failure(e: Throwable) =>
        log.error("Unsuccessful request: ", e)
        throw e
    }

    null
  }

  @throws(classOf[Exception])
  def runJob(runningClass: String, contextName: String, parameters: Map[String, String]) : Job = {
    val body = createParametersString(parameters)
    val pipeline: HttpRequest => Future[Job] = sendReceive ~> unmarshal[Job]
    val response: Future[Job] = pipeline(Post(serverAddress + jobsRoute + "?runningClass=" + runningClass + "&contextName=" + contextName, body))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(job: Job) =>
        return job
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        throw e
      case Failure(e: Throwable) =>
        log.error("Unsuccessful request: ", e)
        throw e
    }

    null
  }

  //  ============  Jars Route  ============
  @throws(classOf[Exception])
  def getJars: JarsInfo = {
    val pipeline: HttpRequest => Future[JarsInfo] = sendReceive ~> unmarshal[JarsInfo]
    val response: Future[JarsInfo] = pipeline(Get(serverAddress + jarsRoute))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(jarsInfo: JarsInfo) =>
        return jarsInfo
      case Failure(e) =>
        log.error("Failed request: ", e)
        throw e
    }

    null
  }

  @throws(classOf[Exception])
  def deleteJar(jarName: String) : Boolean = {
    val pipeline: HttpRequest => Future[SimpleMessage] = sendReceive ~> unmarshal[SimpleMessage]
    val response: Future[SimpleMessage] = pipeline(Delete(serverAddress + jarsRoute + SEPARATOR + jarName))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(simpleMessage: SimpleMessage) =>
        return true
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        return false
      case Failure(e: Throwable) =>
        log.error("Unsuccessful request: ", e)
        throw e
    }

    false
  }

  @throws(classOf[Exception])
  def uploadJar(jarName: String, jarPath: String) : JarInfo = {
    val pipeline: HttpRequest => Future[JarInfo] = sendReceive ~> unmarshal[JarInfo]
    val body = MultipartFormData(Seq(BodyPart(new File(jarPath), jarName, MediaTypes.`application/java-archive`)))
    val response: Future[JarInfo] = pipeline(Post(serverAddress + jarsRoute , body))

    Await.ready(response, Duration.create(30, TimeUnit.SECONDS)).value.get match {
      case Success(jarInfo: JarInfo) =>
        return jarInfo
      case Failure(e: UnsuccessfulResponseException) =>
        log.error("Unsuccessful response: ", e)
        throw e
      case Failure(e: Throwable) =>
        log.error("Unsuccessful request: ", e)
        throw e
    }

    null
  }

  def createParametersString(parameters: Map[String, String]): String =
    parameters.foldLeft("") {
      case (acc, (key, value)) =>
        acc + key + "=" + value + "\n"
    }
}
