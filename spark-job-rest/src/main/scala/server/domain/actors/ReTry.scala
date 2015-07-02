package server.domain.actors

import akka.actor._
import akka.pattern.ask
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/*
 See http://www.codetinkerhack.com/2014/01/re-try-pattern-using-akka-actor-ask.html
 */

object ReTry {
  private case class Retry(originalSender: ActorRef, message: Any, times: Int)

  private case class Response(originalSender: ActorRef, result: Any)

  def props(tries: Int, retryTimeOut: FiniteDuration, retryInterval: FiniteDuration, forwardTo: ActorSelection): Props = Props(new ReTry(tries: Int, retryTimeOut: FiniteDuration, retryInterval: FiniteDuration, forwardTo: ActorSelection))

}

class ReTry(val tries: Int, retryTimeOut: FiniteDuration, retryInterval: FiniteDuration, forwardTo: ActorSelection) extends Actor {

  import context.dispatcher
  import server.domain.actors.ReTry._
  val log = LoggerFactory.getLogger(getClass)

  // Retry loop that keep on Re-trying the request
  def retryLoop: Receive = {

    // Response from future either Success or Failure is a Success - we propagate it back to a original sender
    case Response(originalSender, result) =>
      originalSender ! result
      context stop self

    case Retry(originalSender, message, triesLeft) =>

      // Process (Re)try here. When future completes it sends result to self
      (forwardTo ? message) (retryTimeOut) onComplete {

        case Success(result) =>
          self ! Response(originalSender, result) // sending responses via self synchronises results from futures that may come potentially in any order. It also helps the case when the actor is stopped (in this case responses will become deadletters)

        case Failure(ex) =>
          if (triesLeft - 1 == 0) {// In case of last try and we got a failure (timeout) lets send Retries exceeded error
            self ! Response(originalSender, Failure(new Exception("Retries exceeded")))
          }
          else
            log.error("Error occurred: " + ex)
      }

      // Send one more retry after interval
      if (triesLeft - 1 > 0)
        context.system.scheduler.scheduleOnce(retryInterval, self, Retry(originalSender, message, triesLeft - 1))

    case m @ _ =>
      log.error("No handling defined for message: " + m)

  }

  // Initial receive loop
  def receive: Receive = {

    case message @ _ =>
      context.system.scheduler.scheduleOnce(retryInterval, self, Retry(sender(), message, tries))

      // Lets swap to a retry loop here.
      context.become(retryLoop, discardOld = false)

  }

}
