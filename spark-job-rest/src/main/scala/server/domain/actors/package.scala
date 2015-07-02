package server.domain

import com.typesafe.config.Config

/**
 * Utility functions for actors
 */
package object actors {
  implicit val timeout = config.durations.ask.timeout

  def getValueFromConfig[T](config: Config, configPath: String, defaultValue: T): T ={
    if (config.hasPath(configPath)) config.getAnyRef(configPath).asInstanceOf[T] else defaultValue
  }
}
