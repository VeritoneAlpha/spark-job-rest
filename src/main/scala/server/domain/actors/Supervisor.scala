package server.domain.actors

import scala.concurrent.duration._

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, OneForOneStrategy, Props, actorRef2Scala}

class Supervisor extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: Exception => {
        e.printStackTrace()
        Resume
      }
    }
 
  def receive = {     
      case (p: Props, name: String) => sender ! context.actorOf(p, name)
  }

}