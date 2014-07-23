package spark.jobserver

import akka.actor.{ActorRef, ActorSelection, Props, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import ooyala.common.akka.InstrumentedActor
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import java.net.{URLClassLoader, URL}
import com.atigeo.jobmanager.{ManagerActor, JobManagerParameter}

/** Messages common to all ContextSupervisors */
object ContextSupervisor {
  // Messages/actions
  case object AddContextsFromConfig // Start up initial contexts
  case object ListContexts
  case class AddContext(name: String, contextConfig: Config)
  case class GetAdHocContext(classPath: String, contextConfig: Config)
  case class GetContext(name: String) // returns JobManager, JobResultActor
  case class GetResultActor(name: String)  // returns JobResultActor
  case class StopContext(name: String)

  // Errors/Responses
  case object ContextInitialized
  case class ContextInitError(t: Throwable)
  case object ContextAlreadyExists
  case object NoSuchContext
  case object ContextStopped
}

/**
 * This class starts and stops JobManagers / Contexts in-process.
 * It is responsible for watching out for the death of contexts/JobManagers.
 *
 * == Auto context start configuration ==
 * Contexts can be configured to be created automatically at job server initialization.
 * Configuration example:
 * {{{
 *   spark {
 *     contexts {
 *       olap-demo {
 *         num-cpu-cores = 4            # Number of cores to allocate.  Required.
 *         memory-per-node = 1024m      # Executor memory per node, -Xmx style eg 512m, 1G, etc.
 *       }
 *     }
 *   }
 * }}}
 *
 * == Other configuration ==
 * {{{
 *   spark {
 *     jobserver {
 *       context-creation-timeout = 15 s
 *     }
 *
 *     # Default settings for all context creation
 *     context-settings {
 *       spark.mesos.coarse = true
 *     }
 *   }
 * }}}
 */
class LocalContextSupervisorActor(dao: ActorRef, configFile: String) extends InstrumentedActor {
  import ContextSupervisor._
  import scala.collection.JavaConverters._
  import scala.concurrent.duration._

  val config = context.system.settings.config
  val urls = new Array[URL](2);
  urls(0) = new URL("file://" + config.getString("spark.jobserver.jobmanagerjar"));
  urls(1) = new URL("file://" + config.getString("spark.jobserver.akkajar"));


  val firstClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader().getParent());

  val runnableClass = firstClassLoader.loadClass("com.atigeo.jobmanager.ManagerActorCreator");
  val constr = runnableClass.getConstructor(classOf[URLClassLoader], classOf[String]);


  val runnable = constr.newInstance(firstClassLoader, configFile);
  val run: Runnable = runnable.asInstanceOf[Runnable];
  val thread = new Thread(run);
  thread.setContextClassLoader(firstClassLoader)
  thread.start

  Thread.sleep(3000);

  var port = config.getInt("front.akka.remote.netty.tcp.port")

  val customManager = context.actorSelection("akka.tcp://local@localhost:" + port + "/user/manager");
  customManager ! "from:LocalContextSupervisorActor: Manager Actor Initialized!"


  val defaultContextConfig = config.getConfig("spark.context-settings")
  val contextTimeout = config.getMilliseconds("spark.jobserver.context-creation-timeout").toInt / 1000
  import context.dispatcher   // to get ExecutionContext for futures

  private val contexts = mutable.HashMap.empty[String, ActorSelection]
  private val resultActors = mutable.HashMap.empty[String, ActorSelection]

  // This is for capturing results for ad-hoc jobs. Otherwise when ad-hoc job dies, resultActor also dies,
  // and there is no way to retrieve results.

//  val globalResultActor = context.actorOf(Props[JobResultActor], "global-result-actor")
    val globalResultActorRef = context.actorOf(Props(classOf[JobResultActor], configFile), "global-result-actor")
    val globalResultActor = context.actorSelection("global-result-actor")
//    globalResultActor ! "Initialized globalResultActor in the LocalContextSupervizorActor"
//    globalResultActorRef ! "Ref was hit"

  def wrappedReceive: Receive = {
    case AddContextsFromConfig =>
      addContextsFromConfig(config)

    case ListContexts =>
      sender ! contexts.keys.toSeq

    case AddContext(name, contextConfig) =>
      val originator = sender // Sender is a mutable reference, must capture in immutable val
      val mergedConfig = contextConfig.withFallback(defaultContextConfig)
      if (contexts contains name) {
        originator ! ContextAlreadyExists
      } else {
//        originator ! "sampleMess"
        startContext(name, mergedConfig, false, contextTimeout) { contextMgr =>
          originator ! ContextInitialized
        } { err =>
          originator ! ContextInitError(err)
        }
      }

    case GetAdHocContext(classPath, contextConfig) =>
      val originator = sender // Sender is a mutable reference, must capture in immutable val
      logger.info("Creating SparkContext for adhoc jobs.")

      val mergedConfig = contextConfig.withFallback(defaultContextConfig)

      // Keep generating context name till there is no collision
      var contextName = ""
      do {
        contextName = java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + classPath.replace(".", "-")
      } while (contexts contains contextName)

      // Create JobManagerActor and JobResultActor
      startContext(contextName, mergedConfig, true, contextTimeout) { contextMgr =>
        originator ! (contexts(contextName), resultActors(contextName))
      } { err =>
        originator ! ContextInitError(err)
      }

    case GetResultActor(name) =>
      sender ! resultActors.get(name).getOrElse(globalResultActor)

    case GetContext(name) =>
      if (contexts contains name) {
        sender ! (contexts(name), resultActors(name))
      } else {
        sender ! NoSuchContext
      }

    case StopContext(name) =>
      if (contexts contains name) {
        logger.info("Shutting down context {}", name)
        contexts(name) ! PoisonPill
        contexts.remove(name)
        resultActors.remove(name)
        sender ! ContextStopped
      } else {
        sender ! NoSuchContext
      }
  }

  private def startContext(name: String, contextConfig: Config, isAdHoc: Boolean, timeoutSecs: Int = 1)
                          (successFunc: ActorSelection => Unit)
                          (failureFunc: Throwable => Unit) {
    require(!(contexts contains name), "There is already a context named " + name)
    logger.info("Creating a SparkContext named {}", name)

    implicit val timeout = Timeout(30 seconds)
    println("ADHOC " + isAdHoc)
    val resultActorRef = if (isAdHoc) Some(globalResultActor) else None
    val future = customManager ? new JobManagerParameter(name, contextConfig, isAdHoc, resultActorRef)
    future.onComplete{
      case Failure(e: Exception) =>
        logger.error("Exception because could not initialize JobManagerActor", e)
        failureFunc(e)
      case Success(port:Int) =>
        val ref = context.actorSelection("akka.tcp://" + name + "@localhost:" + port + "/user/" + "JobManagerFor" + name);
        (ref ? JobManagerActor.Initialize)(Timeout(30 seconds)).onComplete {
          case Failure(e: Exception) =>
            logger.error("Exception after sending Initialize to JobManagerActor", e)
            // Make sure we try to shut down the context in case it gets created anyways
            ref ! PoisonPill
            failureFunc(e)
          case Success(JobManagerActor.Initialized(contextAddress)) =>
            logger.info("SparkContext {} initialized", name)
            contexts(name) = ref
            var resultActor:ActorSelection = null
            if(isAdHoc){
              resultActor = globalResultActor
            } else {
              resultActor = context.actorSelection("akka.tcp://"  + name +  "@localhost:" + port + "/user/" + "JobManagerFor" + name + "/" + "result-actor");
            }
            resultActors(name) = resultActor
            successFunc(ref)
          case Success(JobManagerActor.InitError(t)) =>
            ref ! PoisonPill
            failureFunc(t)
          case x =>
            logger.warn("Unexpected message received by startContext: {}", x)
        }
      case x =>
        logger.warn("Unexpected message received by init JobManagerActor: {}", x)
    }
  }

  // Adds the contexts from the config file
  private def addContextsFromConfig(config: Config) {
    for (contexts <- Try(config.getObject("spark.contexts"))) {
      contexts.keySet().asScala.foreach { contextName =>
        val contextConfig = config.getConfig("spark.contexts." + contextName)
          .withFallback(defaultContextConfig)
        startContext(contextName, contextConfig, false, contextTimeout) { ref => } {
          e => logger.error("Unable to start context " + contextName, e)
        }
        Thread sleep 500 // Give some spacing so multiple contexts can be created
      }
    }
  }
}
