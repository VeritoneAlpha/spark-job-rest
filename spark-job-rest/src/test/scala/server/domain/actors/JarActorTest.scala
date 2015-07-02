package server.domain.actors

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import api.responses.JarInfo
import server.domain.actors.JarActor._
import utils.FileUtils

import scala.util.{Random, Success}

/**
 * Test suite for [[JarActor]].
 */
@RunWith(classOf[JUnitRunner])
class JarActorTest extends FunSuite with BeforeAndAfter with ScalaFutures with Matchers {

  val config = ConfigFactory.load()

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)
  implicit val system = ActorSystem("localSystem")

  val jarActor = TestActorRef(new JarActor(config))
  val contextName = "demoContext"

  val jarFolder = config.getString(JarActor.JAR_FOLDER_PROPERTY_PATH)

  before {
    jarActor ! CreateJarFolder
  }

  after {
    jarActor ! DeleteJarFolder
  }

  test("Delete & Create Jar Folder") {

    jarActor ! DeleteJarFolder()

    var future = jarActor ? JarFolderExists()
    val Success(resultNotExists: Boolean) = future.value.get
    resultNotExists should be(false)

    jarActor ! CreateJarFolder(true)

    future = jarActor ? JarFolderExists()
    val Success(resultExists: Boolean) = future.value.get
    resultExists should be(true)

    future = jarActor ? GetAllJarsNames()
    val Success(result: List[String]) = future.value.get
    result should be( Nil )

  }

  test("Write & Delete Jar") {

    val jarName = Random.nextString(5) + ".jar"

    var future = jarActor ? AddJar(jarName, getTestJarBytes)
    val Success(result: Success[JarInfo]) = future.value.get
    result shouldBe an [Success[JarInfo]]

    future = jarActor ? GetAllJarsNames()
    val Success(resultJars: List[String]) = future.value.get
    resultJars should be( List(jarName) )

    future = jarActor ? DeleteJar(jarName)
    val Success(deleteResult: Success[String]) = future.value.get
    deleteResult should be( Success("Jar deleted.") )

    future = jarActor ? GetAllJarsNames()
    val Success(emptyJarList: List[String]) = future.value.get
    emptyJarList should be( Nil )

  }

  test("Write 10 Jars") {
    for( i <- 1 to 10) {
      val jarName = Random.nextString(5) + ".jar"
      var future = jarActor ? AddJar(jarName, getTestJarBytes)
    }

    val future = jarActor ? GetAllJarsNames()
    val Success(emptyJarList: List[String]) = future.value.get
    emptyJarList.size should be( 10 )
  }

  test("Get Classpath For Uploaded Jar"){
    val jarName = Random.nextString(5) + ".jar"
    var future = jarActor ? AddJar(jarName, getTestJarBytes)

    future = jarActor ? GetJarsPathForClasspath(jarName, contextName)
    val Success(result: String) = future.value.get
    result should be( config.getString(JarActor.JAR_FOLDER_PROPERTY_PATH) + File.separator + jarName)
  }

  def createLocalJar(jarName: String) = {
    val jarPath = jarFolder + jarName
    FileUtils.writeToFile(jarName, jarFolder, getTestJarBytes)
  }

  test("Get Classpath For Local Jar"){

    val jarName = "test.jar"
    val jarPath = jarFolder + File.separator + jarName
    createLocalJar(jarName)

    val future = jarActor ? GetJarsPathForClasspath(jarPath, contextName)
    val Success(result: String) = future.value.get
    result should be( jarPath )
  }

//  test("Get Classpath For Hdfs Jar"){
//    TODO: Add test for hdfs jar
//    val jarPath = "/home/ubuntu/test.jar"
//
//    val future = jarActor ? GetJarsPathForClasspath(jarPath)
//    val Success(result: String) = future.value.get
//    result should be( jarPath )
//  }

  test("Get Classpath For Multiple Jars"){

    //    TODO: Add hdfs jar to this test
    val localJarName = "test.jar"
    val jarPath = jarFolder + File.separator + localJarName
    createLocalJar(localJarName)

    val jarName = Random.nextString(5) + ".jar"
    var future = jarActor ? AddJar(jarName, getTestJarBytes)

    future = jarActor ? GetJarsPathForClasspath(jarPath + "," + jarName, contextName)
    val Success(result: String) = future.value.get
    result should be( jarPath + JarActor.CLASSPATH_JAR_SEPARATOR + config.getString(JarActor.JAR_FOLDER_PROPERTY_PATH) + File.separator + jarName)
  }

  test("Get Spark Jars For Uploaded Jar"){
    val jarName = Random.nextString(5) + ".jar"
    var future = jarActor ? AddJar(jarName, getTestJarBytes)

    future = jarActor ? GetJarsPathForSpark(jarName)
    val Success(result: List[String]) = future.value.get
    result should be( List(config.getString(JarActor.JAR_FOLDER_PROPERTY_PATH) + File.separator + jarName))
  }

  test("Get Spark Jars For Local Jar"){

    val localJarName = "test.jar"
    val jarPath = jarFolder + File.separator + localJarName
    createLocalJar(localJarName)

    val future = jarActor ? GetJarsPathForSpark(jarPath)
    val Success(result: List[String]) = future.value.get
    result should be( List(jarPath) )
  }

    test("Get Spark Jars For Hdfs Jar"){
      val jarPath = "hdfs://devbox.local:8020/home/ubuntu/test.jar"

      val future = jarActor ? GetJarsPathForSpark(jarPath)
      val Success(result: List[String]) = future.value.get
      result should be( List(jarPath) )
    }

  test("Get Spark Jars For Multiple Jars"){

    val localJarName = "test.jar"
    val jarPath = jarFolder + File.separator + localJarName
    createLocalJar(localJarName)

    val hdfsJarPath = "hdfs://devbox.local:8020/home/ubuntu/test.jar"

    val jarName = Random.nextString(5) + ".jar"
    var future = jarActor ? AddJar(jarName, getTestJarBytes)

    future = jarActor ? GetJarsPathForSpark(jarPath + "," + jarName  + "," + hdfsJarPath)
    val Success(result: List[String]) = future.value.get
    result should be( List(jarPath, config.getString(JarActor.JAR_FOLDER_PROPERTY_PATH) + File.separator + jarName, hdfsJarPath))
  }

  test("Get All Jars Paths For Multiple Jars"){

    val localJarName = "test.jar"
    val jarPath = jarFolder + File.separator + localJarName
    createLocalJar(localJarName)

//    val hdfsJarPath = "hdfs://devbox.local:8020/home/ubuntu/test.jar"

    val jarName = Random.nextString(5) + ".jar"
    var future = jarActor ? AddJar(jarName, getTestJarBytes)

    future = jarActor ? GetJarsPathForAll(jarPath + "," + jarName, contextName)
    val Success(result: ResultJarsPathForAll) = future.value.get
    result.pathForSpark should be( List(jarPath, config.getString(JarActor.JAR_FOLDER_PROPERTY_PATH) + File.separator + jarName))
    result.pathForClasspath should be ( jarPath + JarActor.CLASSPATH_JAR_SEPARATOR + jarFolder + File.separator + jarName)
  }


  def getTestJarBytes: Array[Byte] = {
    val bytes: Array[Byte] = Array(0x50.toByte, 0x4b.toByte, 0x03.toByte, 0x04.toByte)

    val randomBytes = new Array[Byte](20)
    Random.nextBytes(randomBytes)

    bytes ++ randomBytes
  }
}
