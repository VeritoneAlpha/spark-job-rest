package server

import akka.actor.{ActorSelection, ActorSystem, ActorRef}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import server.domain.actors.{JobActor, ContextManagerActor, ContextActor}
import ContextActor.{FailedInit}
import ContextManagerActor._
import JobActor._
import spray.http.{MediaTypes, StatusCodes}
import spray.routing.{Route, SimpleRoutingApp}
import akka.pattern.ask
import server.domain.actors.getValueFromConfig

import scala.concurrent.ExecutionContext
import scala.util.Success
import ExecutionContext.Implicits.global

/**
 * Created by raduc on 03/11/14.
 */
  class Controller(config: Config, contextManagerActor: ActorRef, jobManagerActor: ActorRef, originalSystem: ActorSystem) extends SimpleRoutingApp{

  implicit val system = originalSystem
  implicit val timeout = Timeout(60000)


  val webPort = getValueFromConfig(config, "appConf.web.services.port", 8097)

  val logger = LoggerFactory.getLogger(getClass)
  logger.info("Starting web service.")


  val route = jobRoute  ~ contextRoute ~ indexRoute
  startServer("0.0.0.0", webPort) (route)

  def indexRoute: Route = pathPrefix("index"){
    get {
      complete{
        "Spark Job Rest is up and running!"
      }
    }
  }

  def jobRoute: Route = pathPrefix("job"){
    get{
        parameters('uuid) { uuid =>
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            val resultFuture = jobManagerActor ? JobStatusEnquiry(uuid)
            resultFuture.map{
              case x:JobRunSuccess => ctx.complete(StatusCodes.OK, "Finished")
              case x:JobRunError => ctx.complete(StatusCodes.InternalServerError, s"Error: ${x.errorMessage}")
              case x:JobStarted => ctx.complete(StatusCodes.OK, "Running")
              case x:JobDoesNotExist => ctx.complete(StatusCodes.BadRequest, "JobId does not exist!")
              case x:String => ctx.complete(StatusCodes.BadRequest, x)
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
      val resultFuture = contextManagerActor ? GetAllContexts()
      respondWithMediaType(MediaTypes.`application/json`) { ctx =>
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
}
