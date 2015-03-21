package server.domain.actors

import java.io.{FileOutputStream, File}

import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.Config
import org.apache.commons.io.FileUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}
import server.domain.actors.JarActor._

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
  case class GetJarsPathForClasspath(paths: String)
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
        validateJar(bytes)
        writeToFile(jarName, bytes)
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
    case GetJarsPathForClasspath(path) => {
      var jarClasspath = ""
      path.split(",").foreach { x =>
        jarClasspath += getPathForClasspath(x) + CLASSPATH_JAR_SEPARATOR
      }

      sender ! jarClasspath.substring(0, jarClasspath.size - 1)
    }
    case GetJarsPathForSpark(path) => {
      var jarSparkPathList = ListBuffer[String]()
      path.split(",").foreach { x =>
        jarSparkPathList +=(getPathForSpark(x))
      }

      sender ! jarSparkPathList.toList
    }
    case DeleteJarFolder() => {
      val file = new File(jarFolder)
      if(file.exists()){
        FileUtils.deleteDirectory(file)
      }
    }

    case CreateJarFolder(overwrite: Boolean) => {
      val file = new File(jarFolder)
      if(!file.exists()){
        file.mkdir()
      } else if (overwrite){
        FileUtils.deleteDirectory(file)
        file.mkdir()
      }
    }

    case JarFolderExists() => {
      val file = new File(jarFolder)
      sender ! file.exists()
    }
  }

  def writeToFile(jarName: String, bytes: Array[Byte]): Unit = {
    val fos = new FileOutputStream(jarFolder + File.separator + jarName)
    fos.write(bytes)
    fos.close()
  }

  def copyJarFromHdfs(hdfsPath: String) = {

//    if(!config.hasPath("hdfs.namenode")){
//      println("ERROR: HDFS NameNode is not set in application.conf!")
//      throw new Exception("HDFS NameNode is not set in application.conf!")
//    }

    val conf = new Configuration()
//    conf.set("fs.defaultFS", getValueFromConfig(config, "hdfs.namenode", ""))
    conf.set("fs.defaultFS", hdfsPath)
    val hdfsFileSystem = FileSystem.get(conf)

    hdfsFileSystem.copyToLocalFile(new Path(hdfsPath), new Path(jarFolder))
  }

  def getPathForClasspath(path: String): String = {
    if(path.startsWith("/")){
      return path
    } else {
      var jarName = path
      if (path.startsWith("hdfs")) {
        copyJarFromHdfs(path)
        jarName = path.substring(path.lastIndexOf('\\'))
      }

      val file = new File(jarFolder + jarName)
      if (file.exists()) {
        return jarFolder + path
      }
    }

    throw new Exception(s"Jar $path  could not be resolved.")
  }

  def getPathForSpark(path: String): String = {
    if(path.startsWith("/")){
      return path
    } else if (path.startsWith("hdfs")) {
      return path
    } else {

      val file = new File(jarFolder + path)
      if (file.exists()) {
        return jarFolder + path
      }
    }

    throw new Exception(s"Jar $path  could not be resolved.")
  }

  def validateJar(bytes: Array[Byte]): Unit = {
    // For now just check the first few bytes are the ZIP signature: 0x04034b50 little endian
    if(bytes.size < 4 || bytes(0) != 0x50 || bytes(1) != 0x4b || bytes(2) != 0x03 || bytes(3) != 0x04){
      throw new Exception("Invalid Jar! The jar does not pass jar validation.")
    }
  }
}


