package server.domain.actors

import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.Config

/**
 * Created by raduc on 04/11/14.
 */

object JarActor {
  case class AddJar(jarName: String, bytes: Array[Byte])
  case class BadJar()
  case class NoSuchJar()
  case class DeleteJar(jarName: String)
  case class GetAllJars()
}

class JarActor() extends Actor with ActorLogging{
  override def receive: Receive = {
    case x @ _ => {

    }
  }
}


