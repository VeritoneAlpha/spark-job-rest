package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.util.Timeout

import scala.concurrent.duration._

package object durations {
  val defaultAskTimeout = Timeout(100, TimeUnit.MILLISECONDS)

  val defaultRemoteAskTimeout = Timeout(5, TimeUnit.SECONDS)

  val defaultDbTimeout = 5 seconds

  val databaseInitializationTimeout = Timeout(10, TimeUnit.SECONDS)
}
