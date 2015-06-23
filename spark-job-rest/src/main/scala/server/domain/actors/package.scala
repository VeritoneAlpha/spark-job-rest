package server.domain

import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration._

/**
 * Utility functions for actors
 */
package object actors {
  implicit val timeout: Timeout = 50 seconds

  def getValueFromConfig[T](config: Config, configPath: String, defaultValue: T): T ={
    if (config.hasPath(configPath)) config.getAnyRef(configPath).asInstanceOf[T] else defaultValue
  }
}
