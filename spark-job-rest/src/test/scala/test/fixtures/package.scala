package test

import com.typesafe.config.ConfigFactory
import persistence.schema._

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
  def contextEntity = Context("test context", ContextState.Running, bananaConfig, None, Jars(), nextId)

  /**
   * Random job entity to reduce updates when context schema updates
   */
  def jobEntity(context: Context) = Job(Some(context.id), None, None, "java.utils.UUID", diningConfig, Some(bananaConfig))
}
