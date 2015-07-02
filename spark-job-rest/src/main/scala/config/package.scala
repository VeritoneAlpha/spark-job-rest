import com.typesafe.config.ConfigFactory

/**
 * System wide config and config utils
 */
package object config {
  /**
   * Default application configuration.
   * Loads deployment configuration `deploy.conf` on top of application defaults `application.conf`
   */
  lazy val default = ConfigFactory.load("deploy").withFallback(ConfigFactory.load())

  /**
   * Master application configuration.
   *
   */
  lazy val master = default.getConfig("spark.job.rest.manager")
}
