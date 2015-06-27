package server.domain.actors

import java.util.concurrent.TimeUnit

import akka.util.Timeout

package object durations {
  implicit val defaultAskTimeout = Timeout(100, TimeUnit.MILLISECONDS)
}
