package test

import api.entities.{Jars, JobDetails, ContextState, ContextDetails}
import com.typesafe.config.ConfigFactory

package object fixtures {
  /**
   * Any config we may load from test resources 
   */
  val applicationConfig = ConfigFactory.load()

  /**
   * A small config
   */
  val diningConfig = ConfigFactory.parseString(
    """
      |{
      |  hero.name = "Lizzy"
      |}
    """.stripMargin)

  /**
   * Just a random big config. But:
   * Yay! I've just got that "Gravity's Rainbow" is a sequel of V. since it's about V-2 (how I've missed that before?)
   * @author Mikhail Zyatin
   */
  val bananaConfig = ConfigFactory.parseString(
    """
      |{
      |  hero.name = "Geoffrey Pirate Prentice"
      |}
    """.stripMargin)
    // We want to test against a BIG config
    .withFallback(applicationConfig)

  /**
   * Random context entity to reduce updates when context schema updates
   */
  def contextEntity = ContextDetails("test context", diningConfig, Some(bananaConfig), Jars(), ContextState.Running)

  /**
   * Random job entity to reduce updates when context schema updates
   */
  def jobEntity(context: ContextDetails) = JobDetails(Some(context.id), None, None, "java.utils.UUID", diningConfig, Some(bananaConfig))
}
