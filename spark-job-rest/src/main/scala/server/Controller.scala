package server

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import api.entities.{ContextDetails, JobDetails}
import api.json.JsonProtocol._
import api.responses._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import persistence.services.ContextPersistenceService._
import persistence.services.JobPersistenceService._
import server.domain.actors.ContextActor.FailedInit
import server.domain.actors.ContextManagerActor._
import server.domain.actors.JarActor._
import server.domain.actors.JobActor._
import server.domain.actors.getValueFromConfig
import spray.http._
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.routing.{Route, SimpleRoutingApp}
import utils.DatabaseUtils.dbConnection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Spark-Job-REST HTTP service for Web UI and REST API.
 */
class Controller(val config: Config,
                 contextManagerActor: ActorRef,
                 jobManagerActor: ActorRef,
                 jarActor: ActorRef,
                 connectionProviderActor: ActorRef,
                 originalSystem: ActorSystem) extends SimpleRoutingApp with CORSDirectives {

  implicit val system = originalSystem
  implicit val timeout: Timeout = 60.seconds

  val log = LoggerFactory.getLogger(getClass)
  log.info("Starting web service.")

  var StateKey = "state"
  var ResultKey = "result"

  // Get ip from config, "0.0.0.0" as default
  val webIp = getValueFromConfig(config, "spark.job.rest.appConf.web.services.ip", "0.0.0.0")
  val webPort = getValueFromConfig(config, "spark.job.rest.appConf.web.services.port", 8097)

  val route = jobRoute ~ contextRoute ~ contextHistoryRoute ~ indexRoute ~ jarRoute

  /**
   * Database connection received from connection provider [[server.domain.actors.DatabaseServerActor]]
   */
  var db = dbConnection(connectionProviderActor)

  startServer(webIp, webPort)(route) map {
    case bound => log.info(s"Started web service: $bound")
  } onFailure {
    case e: Exception =>
      log.error("Failed to start Spark-Job-REST web service", e)
      throw e
  }

  def indexRoute: Route = pathPrefix("") {
    pathEnd {
      get {
        getFromResource("webapp/index.html")
      }
    } ~
      options {
        corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET))) {
          complete {
            "OK"
          }
        }
      }
  } ~
    pathPrefix("assets") {
      get {
        getFromResourceDirectory("webapp/assets")
      } ~ options {
        corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET))) {
          complete {
            "OK"
          }
        }
      }
    } ~
    pathPrefix("js") {
      get {
        getFromResourceDirectory("webapp/js")
      } ~ options {
        corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET))) {
          complete {
            "OK"
          }
        }
      }
    } ~
    path("hearbeat") {
      get {
        complete {
          "Spark Job Rest is up and running!"
        } ~ options {
          corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET))) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              ctx.complete(StatusCodes.OK)
            }
          }
        }
      }
    }

  /**
   * All routes related to jobs
   */
  def jobRoute: Route = pathPrefix("jobs") {
    /**
     * Job list route
     */
    pathEnd {
      get {
        corsFilter(List("*")) {
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            allJobs(db) map {
              case jobsDetails => ctx.complete(StatusCodes.OK, jobsDetails map (j => Job.fromJobDetails(j)))
            } onFailure {
              case e: Throwable =>
                ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                log.error("Error listing jobs", e)
            }
          }
        }
      }
    } ~
      /**
       * Job status route
       */
      get {
        path(JavaUUID) { jobId =>
          corsFilter(List("*")) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              jobById(jobId, db) map {
                case Some(jobDetails) => ctx.complete(StatusCodes.OK, Job.fromJobDetails(jobDetails))
                case None => ctx.complete(StatusCodes.BadRequest, ErrorResponse("JobId does not exist!"))
              } onFailure {
                case e: Throwable =>
                  ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                  log.error(s"Error returning jobs $jobId", e)
              }
            }
          }
        }
      } ~
      /**
       * Job submit route
       */
      post {
        parameters('runningClass, 'contextName) { (runningClass, context) =>
          entity(as[String]) { configString =>
            corsFilter(List("*")) {
              respondWithMediaType(MediaTypes.`application/json`) { ctx =>
                Try {
                  ConfigFactory.parseString(configString)
                } match {
                  case Success(requestConfig) =>
                    val jobDetails = JobDetails(runningClass, requestConfig)
                    val createJobRecordFuture = insertJob(jobDetails, db)
                    createJobRecordFuture map {
                      case _ =>
                        val submitJobFuture = jobManagerActor ? RunJob(runningClass, context, requestConfig, jobDetails.id)
                        submitJobFuture map {
                          case JobAccepted => ctx.complete(StatusCodes.OK, Job.fromJobDetails(jobDetails))
                          case NoSuchContext => ctx.complete(StatusCodes.BadRequest, ErrorResponse("No such context."))
                          case e: Throwable => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                          case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
                        }
                    } onFailure {
                      case e: Throwable => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                    }
                  case Failure(e) => ctx.complete(StatusCodes.BadRequest, ErrorResponse("Invalid parameter: " + e.getMessage))
                }
              }
            }
          }
        }
      } ~
      options {
        corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET, HttpMethods.POST))) {
          complete {
            "OK"
          }
        }
      }
  }

  /**
   * Contexts history returns extended information about all ever running contexts.
   */
  def contextHistoryRoute: Route = pathPrefix("history/contexts") {
    /**
     * Returns full information ([[ContextDetails]]) about given context by its ID.
     */
    get {
      path(JavaUUID) { contextId =>
        corsFilter(List("*")) {
          respondWithMediaType(MediaTypes.`application/json`) { ctx =>
            contextById(contextId, db).map {
              case Some(context: ContextDetails) => ctx.complete(StatusCodes.OK, context)
              case None => ctx.complete(StatusCodes.BadRequest, ErrorResponse("No such context."))
              case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
            }
          }
        }
      }
    } ~
      pathEnd {
        /**
         * Returns a list of all contexts (both active and inactive) in a form of [[Context]]
         */
        get {
          corsFilter(List("*")) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              allContexts(db) onComplete {
                case Success(contexts: Array[ContextDetails]) =>
                  ctx.complete(StatusCodes.OK, contexts.map(
                    context => Context.fromContextDetails(context)
                  ))
                case Failure(e: Throwable) => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
              }
            }
          }
        }
      } ~
      options {
        corsFilter(List("*"), HttpHeaders.`Access-Control-Allow-Methods`(Seq(HttpMethods.OPTIONS, HttpMethods.GET))) {
          complete {
            "OK"
          }
        }
      }
  }

  /**
   * All routes related to active contexts.
   */
  def contextRoute: Route = pathPrefix("contexts") {
    post {
      path(Segment) { contextName =>
        entity(as[String]) { configString =>
          corsFilter(List("*")) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              Try {
                // Parse and resolve context configuration.
                // Resolve is important since we are passing config to actor with different environment:
                // all substitutions will be applied now.
                ConfigFactory.parseString(configString).resolve()
              } match {
                case Success(requestConfig) =>
                  val resultFuture = contextManagerActor ? CreateContext(contextName, getValueFromConfig(requestConfig, "jars", ""), requestConfig)
                  resultFuture.map {
                    case context: Context => ctx.complete(StatusCodes.OK, context)
                    case e: FailedInit => ctx.complete(StatusCodes.InternalServerError, ErrorResponse("Failed Init: " + e.message))
                    case ContextAlreadyExists => ctx.complete(StatusCodes.BadRequest, ErrorResponse("Context already exists."))
                    case e: Throwable => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                    case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
                  }
                case Failure(e) => ctx.complete(StatusCodes.BadRequest, ErrorResponse("Invalid parameters: " + e.getMessage))
              }
            }
          }
        }
      }
    } ~
      get {
        path(Segment) { contextName =>
          corsFilter(List("*")) {
            val resultFuture = contextManagerActor ? GetContextInfo(contextName)
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              resultFuture.map {
                case context: Context => ctx.complete(StatusCodes.OK, context)
                case NoSuchContext => ctx.complete(StatusCodes.BadRequest, ErrorResponse("No such context."))
                case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
              }
            }
          }
        }
      } ~
      pathEnd {
        get {
          corsFilter(List("*")) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              val resultFuture = contextManagerActor ? GetAllContextsForClient
              resultFuture.map {
                case contexts: Contexts => ctx.complete(StatusCodes.OK, contexts)
                case e: Throwable => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
              }
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
                case Success => ctx.complete(StatusCodes.OK, SimpleMessage("Context deleted."))
                case NoSuchContext => ctx.complete(StatusCodes.BadRequest, ErrorResponse("No such context."))
                case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
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

  def jarRoute: Route = pathPrefix("jars") {
    post {
      path(Segment) { jarName =>
        entity(as[Array[Byte]]) { jarBytes =>
          corsFilter(List("*")) {
            val resultFuture = jarActor ? AddJar(jarName, jarBytes)
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              resultFuture.map {
                case Success(jarInfo: JarInfo) => ctx.complete(StatusCodes.OK, jarInfo)
                case Failure(e) => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
              }
            }
          }
        }
      } ~
        pathEnd {
          entity(as[MultipartFormData]) { formData =>
            corsFilter(List("*")) {
              respondWithMediaType(MediaTypes.`application/json`) { ctx =>
                formData.fields.foreach {
                  case bodyPart: BodyPart =>
                    val resultFuture = jarActor ? AddJar(bodyPart.filename.get, bodyPart.entity.data.toByteArray)
                    resultFuture.map {
                      case Success(jarInfo: JarInfo) => ctx.complete(StatusCodes.OK, jarInfo)
                      case Failure(e) =>
                        log.error("Error uploading jar: ", e)
                        ctx.complete(StatusCodes.BadRequest, "")
                      case x: Any => ctx.complete(StatusCodes.InternalServerError, "")
                      // TODO: Message is empty due to a bug on the Ui File Upload part. When fixed used ErrorResponse(e.getMessage)
                    }
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
                case Success(message: String) => ctx.complete(StatusCodes.OK, SimpleMessage(message))
                case NoSuchJar() => ctx.complete(StatusCodes.BadRequest, ErrorResponse("No such jar."))
                case e: Throwable => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
              }
            }
          }
        }
      } ~
      pathEnd {
        get {
          corsFilter(List("*")) {
            respondWithMediaType(MediaTypes.`application/json`) { ctx =>
              val future = jarActor ? GetAllJars()
              future.map {
                case jarsInfo: JarsInfo => ctx.complete(StatusCodes.OK, jarsInfo)
                case e: Throwable => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(e.getMessage))
                case x: Any => ctx.complete(StatusCodes.InternalServerError, ErrorResponse(x.toString))
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

}
