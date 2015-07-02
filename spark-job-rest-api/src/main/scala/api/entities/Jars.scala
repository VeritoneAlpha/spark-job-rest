package api.entities

/**
 * Simple wrapper for JARs list persistence.
 * @param list list of JARs
 */
case class Jars(list: List[String] = Nil)

/**
 * Companion object methods for [[Jars]]
 */
object Jars {
  /**
   * Loads [[Jars]] from string
   * @param repr JARSs as string
   * @return deserialized Jars
   */
  def fromString(repr: String): Jars = repr match {
    case "" => Jars()
    case jarsString: String => Jars(jarsString.split(":").toList)
  }
}
