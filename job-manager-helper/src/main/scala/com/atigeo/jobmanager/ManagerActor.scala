package com.atigeo.jobmanager

import akka.event.slf4j.SLF4JLogging
import java.net.{ServerSocket, URL, URLClassLoader}
import akka.actor.{Actor}
import java.io.{File, IOException}
import com.typesafe.config.ConfigFactory

class ManagerActor(var firstClassLoader: URLClassLoader, val configFileString: String) extends Actor with SLF4JLogging {

  val host = "localhost"
//  val config = ConfigFactory.load()
  val configFile = new File(configFileString)
  private val config = ConfigFactory.parseFile(configFile)


  log.info("Manager Actor initialized. Address : " + self.path)

  val messosUrls = new Array[URL](2);
  messosUrls(0) = new URL("file://" + config.getString("spark.jobserver.mesoshelperjar"));
  messosUrls(1) = new URL("file://" + config.getString("spark.jobserver.mesosjar"));

  val messosClassLoader = new URLClassLoader(messosUrls, firstClassLoader);

  println("Starting  the thread that loads mesos native lib.")
  val runnableClassThatLoadsMessos = messosClassLoader.loadClass("com.atigeo.mesos.loader.MessosLoader");
  val constructorMessos = runnableClassThatLoadsMessos.getConstructor();
  val threadMessos = new Thread(constructorMessos.newInstance().asInstanceOf[Runnable]);
  threadMessos.setContextClassLoader(messosClassLoader);
  threadMessos.start();

  val urls = new Array[URL](1);
  urls(0) = new URL("file://" + config.getString("spark.jobserver.jobserverjar"));

  def receive = {
    case param: JobManagerParameter =>
      val port = findAvailablePort()
      val  classloader = new URLClassLoader(urls, messosClassLoader);
      val runnableClass = classloader.loadClass("spark.jobserver.JobManagerActorCreator");
      val constr = runnableClass.getConstructor(classOf[JobManagerParameter], classOf[URLClassLoader], classOf[Integer], classOf[String]);

      val runnable = constr.newInstance(param, classloader, port, configFileString);
      val run: Runnable = runnable.asInstanceOf[Runnable];
      val thread = new Thread(run);
      thread.setContextClassLoader(classloader)
      thread.start

      Thread.sleep(3000);

      context.sender ! port
    case x: String => {
      println(x)
    }
  }

  def findAvailablePort(): Integer = {
    val notFound = true;
    var currentTry = 10000;
    while (notFound) {
      try {
        new ServerSocket(currentTry).close()
        return currentTry
      }
      catch {
        case e: IOException => {
          currentTry += 1
        }
      }
    }
    return 0
  }

}