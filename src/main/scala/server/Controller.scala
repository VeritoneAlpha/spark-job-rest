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
import spray.http.{MultipartFormData, MediaTypes, StatusCodes}
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
  class Controller(config: Config, contextManagerActor: ActorRef, jobManagerActor: ActorRef, jarActor: ActorRef, originalSystem: ActorSystem) extends SimpleRoutingApp{

  implicit val system = originalSystem
  implicit val timeout = Timeout(60000)

  var StateKey = "state"
  var ResultKey = "result"

  // Get ip from config, "0.0.0.0" as default
  val webIp = getValueFromConfig(config, "appConf.web.services.ip", "0.0.0.0")
  val webPort = getValueFromConfig(config, "appConf.web.services.port", 8097)

  val logger = LoggerFactory.getLogger(getClass)
  logger.info("Starting web service.")


  val route = jobRoute  ~ contextRoute ~ indexRoute ~ jarRoute
  startServer(webIp, webPort) (route)

  def indexRoute: Route = pathPrefix("index"){
    get {
      complete{
        "Spark Job Rest is up and running!"
      }
    }
  }

  def jobRoute: Route = pathPrefix("job"){
    get{
        parameters('contextName, 'jobId) { (contextName, jobId) =>
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            val resultFuture = jobManagerActor ? JobStatusEnquiry(contextName, jobId)
            resultFuture.map{
              case x:JobRunSuccess => ctx.complete(StatusCodes.OK, resultToTable("Finished", x.result))
              case x:JobRunError => ctx.complete(StatusCodes.InternalServerError, resultToTable("Error", x.errorMessage))
              case x:JobStarted => ctx.complete(StatusCodes.OK, resultToTable("Running", ""))
              case x:JobDoesNotExist => ctx.complete(StatusCodes.BadRequest, "JobId does not exist!")
              case NoSuchContext => ctx.complete(StatusCodes.BadRequest, "Context does not exist!")
              case e:Throwable => ctx.complete(StatusCodes.InternalServerError, e)
              case x:String => ctx.complete(StatusCodes.InternalServerError, x)
            }
          }
        }
    } ~
    post{
      parameters('runningClass, 'context ) {
          (runningClass, context) =>
        entity(as[String]) { configString =>
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
  }

    def contextRoute : Route = pathPrefix("context"){
    post {
      path(Segment) { contextName =>
        entity(as[String]) { configString =>
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>

          val config = ConfigFactory.parseString(configString)

          val resultFuture = contextManagerActor ? CreateContext(contextName, getValueFromConfig(config, "jars", ""), config)
            resultFuture.map {
              case ContextInitialized(sparkUiPort) => ctx.complete(StatusCodes.OK, sparkUiPort)
              case e:FailedInit => ctx.complete(StatusCodes.InternalServerError, "Failed Init: " + e.message)
              case ContextAlreadyExists => ctx.complete(StatusCodes.BadRequest, "Context already exists.")
              case e @ _ => println(e)
            }
          }
        }
      }
    } ~
    get {
      path(Segment) { contextName =>
        val resultFuture = contextManagerActor ? GetContext(contextName)
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          resultFuture.map{
            case NoSuchContext => ctx.complete(StatusCodes.BadRequest, "No such context.")
            case x:ActorSelection => ctx.complete(StatusCodes.OK, "Context exists.")
          }
        }
        }
    } ~
    get {
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          val resultFuture = contextManagerActor ? GetAllContexts()
          resultFuture.map {
            case s: String => ctx.complete(StatusCodes.OK, s)
            case e: Any => ctx.complete(StatusCodes.InternalServerError, e.toString)
          }
        }
    } ~
    delete {
      path(Segment) { contextName =>
        val resultFuture = contextManagerActor ? DeleteContext(contextName)
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          resultFuture.map{
            case NoSuchContext => ctx.complete(StatusCodes.BadRequest, "No such context.")
            case Success => ctx.complete(StatusCodes.OK, "Context deleted.")
          }
        }
      }
    }

  }

  def jarRoute : Route = pathPrefix("jar"){
    post {
      path(Segment) { jarName =>
        entity(as[Array[Byte]]) { jarBytes =>
          val resultFuture = jarActor ? AddJar(jarName, jarBytes)
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            resultFuture.map {
              case Failure(e) => ctx.complete(StatusCodes.BadRequest, e)
              case Success(message) => ctx.complete(StatusCodes.OK, message)
            }
          }
        }
      }
    } ~
    delete {
      path(Segment) { jarName =>
        val resultFuture = jarActor ? DeleteJar(jarName)
        respondWithMediaType(MediaTypes.`application/json`) { ctx =>
          resultFuture.map {
            case NoSuchJar => ctx.complete(StatusCodes.BadRequest, "No such jar.")
            case Success => ctx.complete(StatusCodes.OK, "Context deleted.")
            case x:Exception => ctx.complete(StatusCodes.InternalServerError, x.getMessage)
          }
        }
      }
    } ~
    get {
      respondWithMediaType(MediaTypes.`application/json`) { ctx =>
        val future = jarActor ? GetAllJars()
        future.map{
          case list:List[String] => {
            ctx.complete(StatusCodes.BadRequest, list)
          }
        }
      }
    }
  }

  def resultToTable(state: String, result: Any): Map[String, Any] = {
    Map(StateKey -> state, ResultKey -> result)
  }
}
