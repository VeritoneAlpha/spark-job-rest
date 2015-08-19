package server.domain.actors

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, OneForOneStrategy, Props, actorRef2Scala}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
 

class Supervisor extends Actor {

  val log = LoggerFactory.getLogger(getClass)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: Exception => {
        log.error("Exception", e)
        Resume
      }
    }
 
  def receive = {     
      case (p: Props, name: String) => sender ! context.actorOf(p, name)
  }

}