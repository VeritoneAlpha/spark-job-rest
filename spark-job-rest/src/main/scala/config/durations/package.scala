package config

import java.util.concurrent.TimeUnit

import akka.util.Timeout

import scala.concurrent.duration.FiniteDuration

/**
 * Default durations including timeouts, intervals and retries.
 * All values should be functions or values
 */
package object durations {
  private val prefix = s"spark.job.rest.durations"
  private val config = default

  object ask {
    val timeout = Timeout(config.getLong(s"$prefix.ask.timeout"), TimeUnit.MILLISECONDS)
  }

  object init {
    val timeout = Timeout(config.getLong(s"$prefix.init.timeout"), TimeUnit.MILLISECONDS)
    val tries = config.getInt(s"$prefix.init.tries")
  }

  object context {
    val sleep = FiniteDuration(config.getLong(s"$prefix.context.sleep"), TimeUnit.MILLISECONDS)
    val timeout = Timeout(config.getLong(s"$prefix.context.timeout"), TimeUnit.MILLISECONDS)
    val interval = FiniteDuration(config.getLong(s"$prefix.context.interval"), TimeUnit.MILLISECONDS)
    val tries = config.getInt(s"$prefix.context.tries")
  }

  object db {
    val timeout = Timeout(config.getLong(s"$prefix.db.timeout"), TimeUnit.MILLISECONDS)
    val initializationTimeout = Timeout(config.getLong(s"$prefix.db.initialization-timeout"), TimeUnit.MILLISECONDS)

    object connection {
      val timeout = Timeout(config.getLong(s"$prefix.db.connection.timeout"), TimeUnit.MILLISECONDS)
      val tries = config.getInt(s"$prefix.db.connection.tries")
    }
  }
}
