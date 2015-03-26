package server.domain.actors

import java.io.{File}

import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.Config
import server.domain.actors.JarActor._
import utils.{FileUtils, JarUtils}
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
 * Created by raduc on 04/11/14.
 */

object JarActor {
  case class AddJar(jarName: String, bytes: Array[Byte])
  case class NoSuchJar()
  case class DeleteJar(jarName: String)
  case class GetAllJars()
  case class GetJarsPathForClasspath(paths: String, contextName: String)
  case class GetJarsPathForSpark(paths: String)
  case class DeleteJarFolder()
  case class CreateJarFolder(overwrite: Boolean)
  case class JarFolderExists()

  val CLASSPATH_JAR_SEPARATOR = ":"
  val JAR_FOLDER_PROPERTY_PATH = "appConf.jars.path"


}

class JarActor(config: Config) extends Actor with ActorLogging{

  val jarFolder = getValueFromConfig(config, JAR_FOLDER_PROPERTY_PATH, "")

  override def receive: Receive = {
    case AddJar(jarName, bytes) => {
      Try {
        if(!JarUtils.validateJar(bytes)){
          throw new Exception("Jar " + jarName + " is not valid!")
        }
        FileUtils.writeToFile(jarName, jarFolder, bytes)
      } match {
        case Success(v) => sender ! ("Jar successfully saved.")
        case Failure(e) => sender ! Failure(e)
      }
    }
    case DeleteJar(jarName) => {
      val file = new File(jarFolder + File.separator + jarName)
      if(file.exists()){
        file.delete()
        sender ! "Jar successfully deleted."
      } else {
        sender ! NoSuchJar()
      }
    }
    case GetAllJars() => {
      val folderJar = new File(jarFolder)
      val files = folderJar.listFiles().map(_.getName).filter(_.endsWith(".jar")).toList
      sender ! files
    }
    case GetJarsPathForClasspath(path, contextName) => {
      var jarClasspath = ""
      path.split(",").foreach { x =>
        jarClasspath += JarUtils.getPathForClasspath(x, jarFolder, contextName) + CLASSPATH_JAR_SEPARATOR
      }

      sender ! jarClasspath.substring(0, jarClasspath.size - 1)
    }
    case GetJarsPathForSpark(path) => {
      var jarSparkPathList = ListBuffer[String]()
      path.split(",").foreach { x =>
        jarSparkPathList +=(JarUtils.getJarPathForSpark(x, jarFolder))
      }

      sender ! jarSparkPathList.toList
    }
    case DeleteJarFolder() => {
      FileUtils.deleteFolder(jarFolder)
    }

    case CreateJarFolder(overwrite: Boolean) => {
      FileUtils.createFolder(jarFolder, overwrite)
    }

    case JarFolderExists() => {
      val file = new File(jarFolder)
      sender ! file.exists()
    }
  }



}


