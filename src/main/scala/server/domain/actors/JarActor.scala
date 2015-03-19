package server.domain.actors

import java.io.{FileOutputStream, File}

import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.Config
import server.domain.actors.JarActor.{GetAllJars, DeleteJar, AddJar}

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

class JarActor(config: Config) extends Actor with ActorLogging{

  val jarFolder = getValueFromConfig(config, "appConf.jars.path", "")

  override def receive: Receive = {
    case AddJar(jarName, bytes) => {
//      Util.validateJar(bytes)
      try {
        val fos = new FileOutputStream(jarFolder + File.separator + jarName);
        fos.write(bytes);
        fos.close()
      }
    }
    case DeleteJar(jarName) => {
      val file = new File(jarFolder + File.separator + jarName)
      if(file.exists()){
        file.delete()
      }
    }
    case GetAllJars() => {
      val folderJar = new File(jarFolder)
      val files = folderJar.listFiles().filter(_.getName.endsWith(".jar"))
      sender ! files
    }
  }
}


