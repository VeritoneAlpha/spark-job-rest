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
  case class GetJarsPathForAll(paths: String, contextName: String)
  case class DeleteJarFolder()
  case class CreateJarFolder(overwrite: Boolean)
  case class JarFolderExists()
  case class ResultJarsPathForAll(pathForClasspath: String, pathForSpark: List[String])
  case class JarInfo(name: String, size: Long, timestamp: Long)

  val CLASSPATH_JAR_SEPARATOR = ":"
  val JAR_FOLDER_PROPERTY_PATH = "appConf.jars.path"


}

class JarActor(config: Config) extends Actor with ActorLogging{

  val jarFolder = getValueFromConfig(config, JAR_FOLDER_PROPERTY_PATH, "")
  FileUtils.createFolder(jarFolder, false)

  override def receive: Receive = {
    case AddJar(jarName, bytes) => {
      println(s"Received AddJar request for jar $jarName")
      Try {
        if(!JarUtils.validateJar(bytes)){
          println("Jar " + jarName + " is not valid!")
          throw new Exception("Jar " + jarName + " is not valid!")
        }
        FileUtils.writeToFile(jarName, jarFolder, bytes)
      } match {
        case Success(v) => sender ! Success("Jar successfully saved.")
        case Failure(e) => sender ! Failure(e)
      }
    }
    case DeleteJar(jarName) => {
      val file = new File(jarFolder + File.separator + jarName)
      if(file.exists()){
        file.delete()
        sender ! Success("Jar deleted.")
      } else {
        sender ! NoSuchJar()
      }
    }
    case GetAllJars() => {
      val folderJar = new File(jarFolder)
      val files = folderJar.listFiles()
      if(files != null){
        val jarInfos = files.map(jarFile => JarInfo(jarFile.getName, jarFile.length, jarFile.lastModified)).filter(_.name.endsWith(".jar")).toList
        sender ! jarInfos
      } else {
        sender ! List()
      }
    }
    case GetJarsPathForClasspath(path, contextName) => {
      sender ! getJarsPathForClasspath(path, contextName)
    }
    case GetJarsPathForSpark(path) => {
      sender ! getJarsPathForSpark(path)
    }

    case GetJarsPathForAll(paths: String, contextName: String) => {
      sender ! ResultJarsPathForAll(getJarsPathForClasspath(paths, contextName), getJarsPathForSpark(paths))
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


  def getJarsPathForSpark(path: String): List[String] = {
    var jarSparkPathList = ListBuffer[String]()
    path.split(",").foreach { x =>
      jarSparkPathList += (JarUtils.getJarPathForSpark(x, jarFolder))
    }
    jarSparkPathList.toList
  }

  def getJarsPathForClasspath(path: String, contextName: String): String = {
    var jarClasspath = ""
    path.split(",").foreach { x =>
      jarClasspath += JarUtils.getPathForClasspath(x, jarFolder, contextName) + CLASSPATH_JAR_SEPARATOR
    }
    jarClasspath.substring(0, jarClasspath.size - 1)
  }
}


