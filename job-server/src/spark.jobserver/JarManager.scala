package spark.jobserver

import akka.actor.ActorRef
import akka.util.Timeout
import ooyala.common.akka.InstrumentedActor
import org.joda.time.DateTime
import akka.pattern.ask
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import spark.jobserver.io.{GetApps, SaveJar}

// Messages to JarManager actor
case class StoreJar(appName: String, jarBytes: Array[Byte])
case object ListJars

// Responses
case object InvalidJar
case object JarStored

/**
 * An Actor that manages the jars stored by the job server.   It's important that threads do not try to
 * load a class from a jar as a new one is replacing it, so using an actor to serialize requests is perfect.
 */
class JarManager(jobDaoRef: ActorRef) extends InstrumentedActor {
  implicit val timeout = Timeout(5 , TimeUnit.SECONDS)

  override def wrappedReceive: Receive = {
    case ListJars => sender ! createJarsList()

    case StoreJar(appName, jarBytes) =>
      logger.info("Storing jar for app {}, {} bytes", appName, jarBytes.size)
      if (!JarUtils.validateJarBytes(jarBytes)) {
        sender ! InvalidJar
      } else {
        val uploadTime = DateTime.now()
        jobDaoRef ! SaveJar(appName, uploadTime, jarBytes)
        sender ! JarStored
      }
  }

  private def createJarsList():Map[String, DateTime] = {

      val future = jobDaoRef ? GetApps()
      val result = Await.result(future, timeout.duration).asInstanceOf[Map[String, DateTime]]
      result
  }
}
