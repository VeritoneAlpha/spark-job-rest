package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.util.Timeout

import scala.concurrent.duration._

package object durations {
  val defaultAskTimeout = Timeout(100, TimeUnit.MILLISECONDS)

  val defaultDbTimeout = 100 millis

  val databaseInitializationTimeout = Timeout(1, TimeUnit.SECONDS)
}
