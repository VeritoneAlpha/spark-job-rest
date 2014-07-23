package com.atigeo.jobmanager

import java.net.URLClassLoader
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import java.io.File

class ManagerActorCreator(var firstClassLoader: URLClassLoader, configString: String) extends Runnable  {
  def run() {

    val defaultConfig = ConfigFactory.load();
    val configFile = new File(configString)
    val conf = ConfigFactory.parseFile(configFile).withFallback(defaultConfig)

    implicit val system = ActorSystem("local", conf.getConfig("front").withFallback(conf), firstClassLoader)

    val a = system.actorOf(Props(classOf[ManagerActor], firstClassLoader, configString), name = "manager")
    println("Manager Actor path : " + a.path)
  }
}

