import com.typesafe.config.ConfigRenderOptions

package object api {
  /**
   * This render potions used to render configs to database.
   */
  val configRenderingOptions = ConfigRenderOptions.concise()
}
