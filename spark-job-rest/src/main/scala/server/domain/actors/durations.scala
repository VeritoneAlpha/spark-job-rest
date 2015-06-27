package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.util.Timeout

object durations {
  implicit val defaultAskTimeout = Timeout(100, TimeUnit.MILLISECONDS)
}
