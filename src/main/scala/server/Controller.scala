package server

import akka.actor.{ActorSelection, ActorSystem, ActorRef}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import server.domain.actors.JarActor._
import server.domain.actors.{JobActor, ContextManagerActor, ContextActor}
import ContextActor.{FailedInit}
import ContextManagerActor._
import JobActor._
import spray.http._
import spray.routing.{Route, SimpleRoutingApp}
import akka.pattern.ask
import server.domain.actors.getValueFromConfig

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import ExecutionContext.Implicits.global
import JsonUtils._
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.json.DefaultJsonProtocol._

/**
 * Created by raduc on 03/11/14.
 */
  class Controller(config: Config, contextManagerActor: ActorRef, jobManagerActor: ActorRef, jarActor: ActorRef, originalSystem: ActorSystem)
    extends SimpleRoutingApp with CORSDirectives{

  implicit val system = originalSystem
  implicit val timeout = Timeout(60000)

  val log = LoggerFactory.getLogger(getClass)
  log.info("Starting web service.")

  var StateKey = "state"
  var ResultKey = "result"

  // Get ip from config, "0.0.0.0" as default
  val webIp = getValueFromConfig(config, "appConf.web.services.ip", "0.0.0.0")
  val webPort = getValueFromConfig(config, "appConf.web.services.port", 8097)


  val route = jobRoute  ~ contextRoute ~ indexRoute ~ jarRoute
  startServer(webIp, webPort) (route)

  log.info("Started web service.")

  def indexRoute: Route = pathPrefix("index"){
    get {
      complete{
        "Spark Job Rest is up and running!"
      }
    } ~ options {
      corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET, HttpMethods.POST, HttpMethods.DELETE))) {
        complete {
          "OK"
        }
      }
    }
  }

  def jobRoute: Route = path("jobs"){
    get {
      corsFilter(List("*")) {
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          val resultFuture = jobManagerActor ? GetAllJobsStatus()
          resultFuture.map {
            case x: List[Any] => ctx.complete(StatusCodes.OK, x)
            case e: Throwable => ctx.complete(StatusCodes.InternalServerError, e)
            case x: String => ctx.complete(StatusCodes.InternalServerError, x)
          }
        }
      }
    } ~
    get {
      path(Segment) { jobId =>
        parameters('contextName) { contextName =>
          corsFilter(List("*")) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              val resultFuture = jobManagerActor ? JobStatusEnquiry(contextName, jobId)
              resultFuture.map {
                case x: JobRunSuccess => ctx.complete(StatusCodes.OK, resultToTable("Finished", x.result))
                case x: JobRunError => ctx.complete(StatusCodes.InternalServerError, resultToTable("Error", x.errorMessage))
                case x: JobStarted => ctx.complete(StatusCodes.OK, resultToTable("Running", ""))
                case x: JobDoesNotExist => ctx.complete(StatusCodes.BadRequest, "JobId does not exist!")
                case NoSuchContext => ctx.complete(StatusCodes.BadRequest, "Context does not exist!")
                case e: Throwable => ctx.complete(StatusCodes.InternalServerError, e)
                case x: String => ctx.complete(StatusCodes.InternalServerError, x)
              }
            }
          }
        }
      }
    } ~
    post {
      parameters('runningClass, 'context) { (runningClass, context) =>
        entity(as[String]) { configString =>
          corsFilter(List("*")) {
            val config = ConfigFactory.parseString(configString)
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              val resultFuture = jobManagerActor ? RunJob(runningClass, context, config)
              resultFuture.map {
                case x: String => ctx.complete(StatusCodes.OK, x)
                case e: Error => ctx.complete(StatusCodes.InternalServerError, e)
                case NoSuchContext => ctx.complete(StatusCodes.BadRequest, "No such context.")
              }
            }
          }
        }
      }
    } ~
      options {
        corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET, HttpMethods.POST, HttpMethods.DELETE))) {
          complete {
            "OK"
          }
        }
      }

  }

    def contextRoute : Route = pathPrefix("contexts"){
    post {
      path(Segment) { contextName =>
        entity(as[String]) { configString =>
          corsFilter(List("*")) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>

              val config = ConfigFactory.parseString(configString)

              val resultFuture = contextManagerActor ? CreateContext(contextName, getValueFromConfig(config, "jars", ""), config)
              resultFuture.map {
                case ContextInitialized(sparkUiPort) => ctx.complete(StatusCodes.OK, sparkUiPort)
                case e: FailedInit => ctx.complete(StatusCodes.InternalServerError, "Failed Init: " + e.message)
                case ContextAlreadyExists => ctx.complete(StatusCodes.BadRequest, "Context already exists.")
              }
            }
          }
        }
      }
    } ~
    get {
      path(Segment) { contextName =>
        corsFilter(List("*")) {
          val resultFuture = contextManagerActor ? GetContext(contextName)
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            resultFuture.map {
              case NoSuchContext => ctx.complete(StatusCodes.BadRequest, "No such context.")
              case x: ActorSelection => ctx.complete(StatusCodes.OK, "Context exists.")
            }
          }
        }
      }
    } ~
    get {
      corsFilter(List("*")) {
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          val resultFuture = contextManagerActor ? GetAllContextsForClient()
          resultFuture.map {
            case s: List[Any] => ctx.complete(StatusCodes.OK, s)
            case e: Any => ctx.complete(StatusCodes.InternalServerError, e.toString)
          }
        }
      }
    } ~
    delete {
      path(Segment) { contextName =>
        corsFilter(List("*")) {
          val resultFuture = contextManagerActor ? DeleteContext(contextName)
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            resultFuture.map {
              case NoSuchContext => ctx.complete(StatusCodes.BadRequest, "No such context.")
              case Success => ctx.complete(StatusCodes.OK, "Context deleted.")
            }
          }
        }
      }
    } ~ options {
      corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET, HttpMethods.POST, HttpMethods.DELETE))) {
        complete {
          "OK"
        }
      }
    }

  }

  def jarRoute : Route = pathPrefix("jars"){
    post {
      path(Segment) { jarName =>
        entity(as[Array[Byte]]) { jarBytes =>
          corsFilter(List("*")) {
            val resultFuture = jarActor ? AddJar(jarName, jarBytes)
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              resultFuture.map {
                case Failure(e) => ctx.complete(StatusCodes.InternalServerError, e.getMessage)
                case Success(message: String) => ctx.complete(StatusCodes.OK, message)
              }
            }
          }
        }
      }
    } ~
    delete {
      path(Segment) { jarName =>
        corsFilter(List("*")) {
          val resultFuture = jarActor ? DeleteJar(jarName)
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            resultFuture.map {
              case NoSuchJar() => ctx.complete(StatusCodes.BadRequest, "No such jar.")
              case Success(message: String) => ctx.complete(StatusCodes.OK, message)
              case x: Exception => ctx.complete(StatusCodes.InternalServerError, x.getMessage)
            }
          }
        }
      }
    } ~
    get {
      corsFilter(List("*")) {
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          val future = jarActor ? GetAllJars()
          future.map {
            case list: List[Any] => {
              ctx.complete(StatusCodes.OK, list)
            }
          }
        }
      }
    } ~ options {
      corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET, HttpMethods.POST, HttpMethods.DELETE))) {
        complete {
          "OK"
        }
      }
    }
  }

  def resultToTable(state: String, result: Any): Map[String, Any] = {
    Map(StateKey -> state, ResultKey -> result)
  }
}
