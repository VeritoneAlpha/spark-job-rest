package server.domain.actors

import akka.actor.Actor
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessBuilder, ProcessLogger}

object ContextProcessActor {
  case object Terminate
}

class ContextProcessActor(processBuilder: ProcessBuilder, contextName: String) extends Actor {
  import ContextProcessActor._

  val log = LoggerFactory.getLogger(s"$getClass::$contextName")

  class Slf4jProcessLogger extends ProcessLogger {
    def out(line: => String): Unit = log.info(line)
    def err(line: => String): Unit = log.error(line)
    def buffer[T](f: => T): T = f
  }

  val process: Process = processBuilder.run(new Slf4jProcessLogger)

  context.system.scheduler.scheduleOnce(1 seconds) {
    val statusCode = process.exitValue()

    if (statusCode < 0) {
      log.error(s"Context $contextName exit with error code $statusCode.")
    } else {
      log.info(s"Context process exit with status $statusCode")
    }

    context.parent ! ContextManagerActor.ContextProcessTerminated(contextName, statusCode)
    context.system.stop(self)
  }

  def receive: Receive = {
    case Terminate =>
      log.info(s"Received Terminate message")
      context.system.scheduler.scheduleOnce(5 seconds) {
        process.destroy()
        context.system.stop(self)
      }
  }
}
