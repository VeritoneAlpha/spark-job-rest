package server.domain.actors

import java.io.{ BufferedOutputStream, File, FileOutputStream }

import scala.collection.mutable.{ HashMap, SynchronizedMap }
import scala.util.{ Failure, Success }

import akka.actor.{ Actor, ActorLogging }
import com.typesafe.config.Config

import server.domain.actors.JarManagerActor._

object JarManagerActor {
  case class AddJar(replace: Boolean, jarName: String, storePath: String, jarBytes: Array[Byte])
  case class DeleteJar(jarName: String)
  case class GetJars(jarNames: String)
  case class GetAllJars()
  case class NoSuchJar()
  case class JarAlreadyExists()
  case class InvalidJar()
}

class JarManagerActor(config: Config) extends Actor with ActorLogging {

  val jarMap = new HashMap[String, String]() with SynchronizedMap[String, String]

  override def receive: Receive = {

    case AddJar(replace, jarName, storePath, jarBytes) => {
      println(s"Received AddJar message : jarName=$jarName storePath=$storePath bytes=" + jarBytes.size)
      if (jarMap.contains(jarName) && !replace) {
        sender ! JarAlreadyExists
      } else if (!validateJarBytes(jarBytes)) {
        sender ! InvalidJar
      } else {
        val outFile = new File(storePath, jarName + ".jar")
        val buffOutStream = new BufferedOutputStream(new FileOutputStream(outFile))
        try {
          buffOutStream.write(jarBytes)
          buffOutStream.flush
          jarMap.put(jarName, outFile.getAbsolutePath)
        } finally {
          buffOutStream.close
        }
        sender ! Success
      }
    }

    case DeleteJar(jarName) => {
      println(s"Received DeleteJar message : jarName=$jarName")
      if (jarMap.contains(jarName)) {
        val storePath = jarMap.remove(jarName).get
        val jarFile = new File(storePath)
        if (jarFile.delete) {
          sender ! Success
        } else {
          sender ! Failure
        }
      } else {
        sender ! NoSuchJar
      }
    }

    case GetJars(jarNames) => {
      import scala.collection.mutable.ArrayBuffer
      println(s"Received GetJars message : jarNames=$jarNames")
      val jarNameArr = jarNames.split(":")
      val arrBuff = new ArrayBuffer[String]
      for (jarName <- jarNameArr) {
        jarMap.get(jarName) match {
          case Some(s) => arrBuff.+=(s)
          case None    => println(s"$jarName did not add.")
        }
      }
      sender ! arrBuff.toArray[String].mkString(":")
    }

    case GetAllJars() => {
      println(s"Received GetAllJars message.")
      sender ! jarMap.values.mkString(",")
    }
  }

  def validateJarBytes(jarBytes: Array[Byte]): Boolean = {
    jarBytes.size > 4 &&
      // For now just check the first few bytes are the ZIP signature: 0x04034b50 little endian
      jarBytes(0) == 0x50 && jarBytes(1) == 0x4b && jarBytes(2) == 0x03 && jarBytes(3) == 0x04
  }
}